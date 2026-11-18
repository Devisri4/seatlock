package com.devisri.seatlock.hold;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Seat holds in this server's memory, for local runs and tests where no Redis is configured.
 * Same rules as {@link RedisSeatHoldStore}: {@link ConcurrentHashMap#compute} locks the seat's entry
 * while deciding, so two threads can't both take a seat. Only works with a single server.
 */
public class InMemorySeatHoldStore implements SeatHoldStore {

    private final Map<Long, Hold> holds = new ConcurrentHashMap<>();
    private final Clock clock;

    public InMemorySeatHoldStore(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Optional<Hold> hold(long seatId, String guestId, Duration ttl) {
        Instant now = clock.instant();
        holds.values().removeIf(h -> h.isExpiredAt(now)); // Redis drops expired keys by itself; here we tidy up
        Hold mine = new Hold(guestId, now.plus(ttl));
        Hold winner = holds.compute(seatId, (id, current) ->
                current == null || current.isExpiredAt(now) || current.guestId().equals(guestId) ? mine : current);
        return winner == mine ? Optional.of(mine) : Optional.empty();
    }

    @Override
    public boolean release(long seatId, String guestId) {
        AtomicBoolean removed = new AtomicBoolean();
        holds.computeIfPresent(seatId, (id, current) -> {
            if (current.guestId().equals(guestId)) {
                removed.set(!current.isExpiredAt(clock.instant()));
                return null;
            }
            return current;
        });
        return removed.get();
    }

    @Override
    public Map<Long, Hold> holds(Collection<Long> seatIds) {
        Instant now = clock.instant();
        Map<Long, Hold> result = new HashMap<>();
        for (Long seatId : seatIds) {
            Hold hold = holds.get(seatId);
            if (hold != null && !hold.isExpiredAt(now)) {
                result.put(seatId, hold);
            }
        }
        return result;
    }
}
