package com.devisri.seatlock.booking;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.devisri.seatlock.booking.BookingDtos.BookingView;
import com.devisri.seatlock.hold.Hold;
import com.devisri.seatlock.hold.SeatHoldStore;
import com.devisri.seatlock.hold.SeatNotFoundException;
import com.devisri.seatlock.hold.SeatUnavailableException;
import com.devisri.seatlock.live.SeatEvents;
import com.devisri.seatlock.show.Seat;
import com.devisri.seatlock.show.SeatRepository;
import com.devisri.seatlock.show.SeatState;
import com.devisri.seatlock.show.SeatStatus;
import com.devisri.seatlock.show.Show;
import com.devisri.seatlock.show.ShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Turns held seats into a paid booking (payment is simulated). Three lines of defence against double booking:
 * <ol>
 *   <li>Redis hold: you can only pay for seats you hold, and only one guest can hold a seat.</li>
 *   <li>Optimistic locking: marking a seat SOLD fails if another transaction changed it first.</li>
 *   <li>Database constraints: booking_seats.seat_id is a primary key and the idempotency key is unique.</li>
 * </ol>
 */
@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    /** The booking, and whether this request created it (false = same Idempotency-Key seen before). */
    public record BookingResult(BookingView booking, boolean created) {
    }

    private final BookingRepository bookings;
    private final SeatRepository seats;
    private final ShowRepository shows;
    private final SeatHoldStore holds;
    private final TransactionTemplate transaction;
    private final SeatEvents events;
    private final Clock clock;

    public BookingService(BookingRepository bookings, SeatRepository seats, ShowRepository shows, SeatHoldStore holds,
                          TransactionTemplate transaction, SeatEvents events, Clock clock) {
        this.bookings = bookings;
        this.seats = seats;
        this.shows = shows;
        this.holds = holds;
        this.transaction = transaction;
        this.events = events;
        this.clock = clock;
    }

    public BookingResult book(long showId, UUID guestId, UUID idempotencyKey, Collection<Long> requestedSeatIds) {
        String guest = guestId.toString();
        String key = idempotencyKey.toString();
        Set<Long> seatIds = new TreeSet<>(requestedSeatIds);

        // A retry or double click with the same key gets the booking that was already made.
        Optional<Booking> earlier = bookings.findByIdempotencyKey(key);
        if (earlier.isPresent()) {
            return replay(earlier.get(), showId, guest, seatIds);
        }

        List<Seat> wanted = seatsOfShow(showId, seatIds);

        Booking booking;
        try {
            checkHeldByGuest(wanted, guest);
            booking = transaction.execute(tx -> {
                int total = 0;
                for (Seat seat : seats.findAllById(seatIds)) { // fresh copies inside the transaction
                    if (seat.getStatus() == SeatStatus.SOLD) {
                        throw new SeatUnavailableException("Seat " + seat.label() + " is already sold");
                    }
                    seat.markSold();
                    total += seat.getPriceCents();
                }
                return bookings.save(new Booking(showId, guest, key, total, clock.instant(), seatIds));
            });
        } catch (SeatUnavailableException | DataAccessException | TransactionException failed) {
            // Before saying no, check whether a request with the same Idempotency-Key won meanwhile
            // (a double click: the first click sold the seats and released the holds). Then it's a retry, not a loser.
            Optional<Booking> sameKey = bookings.findByIdempotencyKey(key);
            if (sameKey.isPresent()) {
                return replay(sameKey.get(), showId, guest, seatIds);
            }
            if (failed instanceof SeatUnavailableException refused) {
                throw refused;
            }
            // Optimistic lock or unique constraint: another booking took one of these seats in the same moment.
            log.info("Booking refused, seats {} were sold concurrently: {}", seatIds, failed.getClass().getSimpleName());
            throw new SeatUnavailableException("Sorry, one of these seats was just sold to someone else");
        }

        for (Long seatId : seatIds) {
            holds.release(seatId, guest); // the seat is SOLD now; the hold is no longer needed
            events.seatChanged(showId, seatId, SeatState.SOLD);
        }
        return new BookingResult(view(booking), true);
    }

    /** All bookings of this guest, newest first. */
    public List<BookingView> myBookings(UUID guestId) {
        return bookings.findByGuestIdOrderByCreatedAtDescIdDesc(guestId.toString()).stream().map(this::view).toList();
    }

    private BookingResult replay(Booking earlier, long showId, String guest, Set<Long> seatIds) {
        if (!earlier.getGuestId().equals(guest) || earlier.getShowId() != showId || !earlier.getSeatIds().equals(seatIds)) {
            throw new IdempotencyKeyReusedException();
        }
        return new BookingResult(view(earlier), false);
    }

    private List<Seat> seatsOfShow(long showId, Set<Long> seatIds) {
        Map<Long, Seat> found = seats.findAllById(seatIds).stream()
                .filter(seat -> seat.getShowId() == showId)
                .collect(Collectors.toMap(Seat::getId, Function.identity()));
        for (Long id : seatIds) {
            if (!found.containsKey(id)) {
                throw new SeatNotFoundException(showId, id);
            }
        }
        return List.copyOf(found.values());
    }

    private void checkHeldByGuest(List<Seat> wanted, String guest) {
        Map<Long, Hold> current = holds.holds(wanted.stream().map(Seat::getId).toList());
        for (Seat seat : wanted) {
            if (seat.getStatus() == SeatStatus.SOLD) {
                throw new SeatUnavailableException("Seat " + seat.label() + " is already sold");
            }
            Hold hold = current.get(seat.getId());
            if (hold == null || !hold.guestId().equals(guest)) {
                throw new SeatUnavailableException("Seat " + seat.label() + " is not held by you (the hold may have expired)");
            }
        }
    }

    private BookingView view(Booking booking) {
        Show show = shows.findById(booking.getShowId()).orElseThrow();
        List<String> labels = seats.findAllById(booking.getSeatIds()).stream()
                .sorted((a, b) -> a.getRowLabel().equals(b.getRowLabel())
                        ? Integer.compare(a.getSeatNumber(), b.getSeatNumber())
                        : a.getRowLabel().compareTo(b.getRowLabel()))
                .map(Seat::label)
                .toList();
        return new BookingView(booking.getId(), show.getId(), show.getTitle(), show.getVenue(), show.getStartsAt(),
                labels, booking.getTotalCents(), booking.getCreatedAt());
    }
}
