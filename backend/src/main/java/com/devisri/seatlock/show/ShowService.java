package com.devisri.seatlock.show;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.devisri.seatlock.hold.Hold;
import com.devisri.seatlock.hold.SeatHoldStore;
import com.devisri.seatlock.show.SeatRepository.SeatStats;
import com.devisri.seatlock.show.ShowDtos.SeatRow;
import com.devisri.seatlock.show.ShowDtos.SeatView;
import com.devisri.seatlock.show.ShowDtos.ShowDetail;
import com.devisri.seatlock.show.ShowDtos.ShowSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ShowService {

    private final ShowRepository shows;
    private final SeatRepository seats;
    private final SeatHoldStore holdStore;
    private final Clock clock;

    public ShowService(ShowRepository shows, SeatRepository seats, SeatHoldStore holdStore, Clock clock) {
        this.shows = shows;
        this.seats = seats;
        this.holdStore = holdStore;
        this.clock = clock;
    }

    /** Shows that haven't started yet, soonest first, with seat counts. Two queries in total, however many shows. */
    public List<ShowSummary> upcomingShows() {
        List<Show> upcoming = shows.findByStartsAtAfterOrderByStartsAtAsc(Instant.now(clock));
        if (upcoming.isEmpty()) {
            return List.of();
        }
        Map<Long, SeatStats> stats = seats.statsForShows(upcoming.stream().map(Show::getId).toList()).stream()
                .collect(Collectors.toMap(SeatStats::showId, Function.identity()));
        return upcoming.stream().map(show -> {
            SeatStats s = stats.get(show.getId());
            return new ShowSummary(show.getId(), show.getTitle(), show.getVenue(), show.getStartsAt(),
                    s == null ? 0 : s.totalSeats(),
                    s == null ? 0 : s.availableSeats(),
                    s == null ? 0 : s.priceFromCents());
        }).toList();
    }

    /**
     * One show with its seats grouped into rows, front row first. Sold seats come from the database,
     * held seats from the hold store (one round trip for all seats).
     */
    public ShowDetail show(long showId, Optional<UUID> guestId) {
        Show show = shows.findById(showId).orElseThrow(() -> new ShowNotFoundException(showId));
        List<Seat> showSeats = seats.findByShowIdOrderByRowLabelAscSeatNumberAsc(showId);
        Map<Long, Hold> holds = holdStore.holds(showSeats.stream().map(Seat::getId).toList());
        String guest = guestId.map(UUID::toString).orElse(null);

        Map<String, List<SeatView>> byRow = new LinkedHashMap<>();
        for (Seat seat : showSeats) {
            Hold hold = holds.get(seat.getId());
            SeatState state = seat.getStatus() == SeatStatus.SOLD ? SeatState.SOLD
                    : hold != null ? SeatState.HELD : SeatState.AVAILABLE;
            boolean heldByYou = state == SeatState.HELD && hold.guestId().equals(guest);
            byRow.computeIfAbsent(seat.getRowLabel(), row -> new ArrayList<>())
                    .add(new SeatView(seat.getId(), seat.getSeatNumber(), seat.getPriceCents(), state, heldByYou));
        }
        List<SeatRow> rows = byRow.entrySet().stream().map(e -> new SeatRow(e.getKey(), e.getValue())).toList();
        return new ShowDetail(show.getId(), show.getTitle(), show.getVenue(), show.getStartsAt(), rows);
    }
}
