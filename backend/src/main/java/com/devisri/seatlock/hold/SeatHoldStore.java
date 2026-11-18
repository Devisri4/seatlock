package com.devisri.seatlock.hold;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Where seat holds live. Every method is atomic: two guests racing for the same seat can never both get it.
 * Production: {@link RedisSeatHoldStore}. Local runs without Redis: {@link InMemorySeatHoldStore}.
 */
public interface SeatHoldStore {

    /**
     * Holds the seat for this guest for {@code ttl}. If the guest already holds it, the timer starts again.
     *
     * @return the hold if this guest now has the seat, empty if someone else holds it
     */
    Optional<Hold> hold(long seatId, String guestId, Duration ttl);

    /** Releases the seat only if this guest holds it. Returns true if a hold was removed. */
    boolean release(long seatId, String guestId);

    /** Current (not expired) holds for these seats; seats nobody holds are left out. */
    Map<Long, Hold> holds(Collection<Long> seatIds);
}
