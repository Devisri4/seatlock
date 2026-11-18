package com.devisri.seatlock.show;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByShowIdOrderByRowLabelAscSeatNumberAsc(Long showId);

    /** Seat counts and the cheapest price per show, in one query for the whole show list. */
    @Query("""
            select new com.devisri.seatlock.show.SeatRepository$SeatStats(
                s.showId,
                count(s),
                sum(case when s.status = com.devisri.seatlock.show.SeatStatus.AVAILABLE then 1 else 0 end),
                min(s.priceCents))
            from Seat s
            where s.showId in :showIds
            group by s.showId""")
    List<SeatStats> statsForShows(Collection<Long> showIds);

    record SeatStats(Long showId, Long totalSeats, Long availableSeats, Integer priceFromCents) {
    }
}
