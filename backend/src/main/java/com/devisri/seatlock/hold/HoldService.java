package com.devisri.seatlock.hold;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.devisri.seatlock.live.HoldExpiryWatcher;
import com.devisri.seatlock.live.SeatEvents;
import com.devisri.seatlock.show.Seat;
import com.devisri.seatlock.show.SeatRepository;
import com.devisri.seatlock.show.SeatState;
import com.devisri.seatlock.show.SeatStatus;
import org.springframework.stereotype.Service;

@Service
public class HoldService {

    /** A seat this guest holds and when the hold runs out (the browser shows a countdown). */
    public record HoldView(Long seatId, Instant expiresAt) {
    }

    private final SeatRepository seats;
    private final SeatHoldStore store;
    private final HoldProperties properties;
    private final SeatEvents events;
    private final HoldExpiryWatcher expiryWatcher;

    public HoldService(SeatRepository seats, SeatHoldStore store, HoldProperties properties,
                       SeatEvents events, HoldExpiryWatcher expiryWatcher) {
        this.seats = seats;
        this.store = store;
        this.properties = properties;
        this.events = events;
        this.expiryWatcher = expiryWatcher;
    }

    /** Holds the seat for this guest for 5 minutes, or explains why not (409). Holding it again restarts the timer. */
    public HoldView hold(long showId, long seatId, UUID guestId) {
        Seat seat = seatInShow(showId, seatId);
        if (seat.getStatus() == SeatStatus.SOLD) {
            throw new SeatUnavailableException("Seat " + label(seat) + " is already sold");
        }
        Hold hold = store.hold(seatId, guestId.toString(), properties.duration())
                .orElseThrow(() -> new SeatUnavailableException("Seat " + label(seat) + " is held by someone else"));

        // Check again now that we hold it: a buyer releases their hold right after paying, so the seat
        // may have been sold between the check above and our hold. Never leave a hold on a sold seat.
        if (seats.findById(seatId).map(Seat::getStatus).orElse(SeatStatus.SOLD) == SeatStatus.SOLD) {
            store.release(seatId, guestId.toString());
            throw new SeatUnavailableException("Seat " + label(seat) + " is already sold");
        }
        events.seatChanged(showId, seatId, SeatState.HELD);
        expiryWatcher.watch(showId, seatId, properties.duration());
        return new HoldView(seatId, hold.expiresAt());
    }

    /** Lets the seat go. Doing nothing is fine if this guest doesn't hold it (releasing twice is harmless). */
    public void release(long showId, long seatId, UUID guestId) {
        seatInShow(showId, seatId);
        if (store.release(seatId, guestId.toString())) {
            events.seatChanged(showId, seatId, SeatState.AVAILABLE);
        }
    }

    /** The seats of this show that this guest currently holds. */
    public List<HoldView> myHolds(long showId, UUID guestId) {
        List<Long> seatIds = seats.findByShowIdOrderByRowLabelAscSeatNumberAsc(showId).stream().map(Seat::getId).toList();
        String guest = guestId.toString();
        return store.holds(seatIds).entrySet().stream()
                .filter(e -> e.getValue().guestId().equals(guest))
                .map(e -> new HoldView(e.getKey(), e.getValue().expiresAt()))
                .sorted(Comparator.comparing(HoldView::seatId))
                .toList();
    }

    private Seat seatInShow(long showId, long seatId) {
        return seats.findById(seatId)
                .filter(seat -> seat.getShowId() == showId)
                .orElseThrow(() -> new SeatNotFoundException(showId, seatId));
    }

    private static String label(Seat seat) {
        return seat.getRowLabel() + seat.getSeatNumber();
    }
}
