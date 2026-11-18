package com.devisri.seatlock.live;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.devisri.seatlock.hold.SeatHoldStore;
import com.devisri.seatlock.show.SeatRepository;
import com.devisri.seatlock.show.SeatState;
import com.devisri.seatlock.show.SeatStatus;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Redis deletes an expired hold silently, so nobody is told the seat is free again. For every hold this
 * sets a timer for just after it ends; then, if nobody holds the seat (not renewed, not taken by someone
 * else) and it isn't sold, browsers are told it's AVAILABLE. A renewed hold simply gets a later timer.
 */
@Component
public class HoldExpiryWatcher {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryWatcher.class);
    private static final Duration GRACE = Duration.ofMillis(500);

    private final SeatHoldStore holds;
    private final SeatRepository seats;
    private final SeatEvents events;
    private final ScheduledExecutorService timers = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("hold-expiry").daemon().factory());

    public HoldExpiryWatcher(SeatHoldStore holds, SeatRepository seats, SeatEvents events) {
        this.holds = holds;
        this.seats = seats;
        this.events = events;
    }

    /** Check this seat once the hold that was just made should have run out. */
    public void watch(long showId, long seatId, Duration holdTime) {
        timers.schedule(() -> check(showId, seatId), holdTime.plus(GRACE).toMillis(), TimeUnit.MILLISECONDS);
    }

    void check(long showId, long seatId) {
        try {
            boolean stillHeld = !holds.holds(List.of(seatId)).isEmpty();
            boolean sold = seats.findById(seatId).map(seat -> seat.getStatus() == SeatStatus.SOLD).orElse(true);
            if (!stillHeld && !sold) {
                events.seatChanged(showId, seatId, SeatState.AVAILABLE);
            }
        } catch (RuntimeException e) {
            log.warn("Could not check expired hold on seat {}: {}", seatId, e.toString());
        }
    }

    @PreDestroy
    void stop() {
        timers.shutdownNow();
    }
}
