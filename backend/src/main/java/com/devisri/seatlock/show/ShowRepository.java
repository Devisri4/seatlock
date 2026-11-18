package com.devisri.seatlock.show;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowRepository extends JpaRepository<Show, Long> {

    List<Show> findByStartsAtAfterOrderByStartsAtAsc(Instant time);

    boolean existsByStartsAtAfter(Instant time);
}
