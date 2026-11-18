package com.devisri.seatlock.hold;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Seat holds in Redis. One key per seat ({@code seatlock:hold:seat:42}) whose value is
 * {@code <guestId>|<expiresAtMillis>} and which Redis deletes by itself when the hold expires.
 *
 * <p>Each operation is a small Lua script. Redis runs a script as one step with nothing in between,
 * so "check who holds the seat, then change it" can never be interrupted by another request.
 */
public class RedisSeatHoldStore implements SeatHoldStore {

    static final String KEY_PREFIX = "seatlock:hold:seat:";

    /**
     * KEYS[1] seat key, ARGV[1] guest id, ARGV[2] value to store, ARGV[3] hold time in ms.
     * SET NX PX: take the seat only if nobody has it, and expire it after the hold time (one atomic command).
     * If it's already taken by the same guest, restart the timer. Returns 1 if the guest has the seat, else 0.
     */
    private static final RedisScript<Long> HOLD = RedisScript.of("""
            if redis.call('SET', KEYS[1], ARGV[2], 'NX', 'PX', ARGV[3]) then
              return 1
            end
            local current = redis.call('GET', KEYS[1])
            if current and string.sub(current, 1, #ARGV[1] + 1) == ARGV[1] .. '|' then
              redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
              return 1
            end
            return 0
            """, Long.class);

    /** KEYS[1] seat key, ARGV[1] guest id. Deletes the key only if this guest holds it. */
    private static final RedisScript<Long> RELEASE = RedisScript.of("""
            local current = redis.call('GET', KEYS[1])
            if current and string.sub(current, 1, #ARGV[1] + 1) == ARGV[1] .. '|' then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public RedisSeatHoldStore(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @Override
    public Optional<Hold> hold(long seatId, String guestId, Duration ttl) {
        Instant expiresAt = clock.instant().plus(ttl).truncatedTo(ChronoUnit.MILLIS); // stored in ms
        Long held = redis.execute(HOLD, List.of(key(seatId)),
                guestId, guestId + "|" + expiresAt.toEpochMilli(), String.valueOf(ttl.toMillis()));
        return held != null && held == 1 ? Optional.of(new Hold(guestId, expiresAt)) : Optional.empty();
    }

    @Override
    public boolean release(long seatId, String guestId) {
        Long deleted = redis.execute(RELEASE, List.of(key(seatId)), guestId);
        return deleted != null && deleted == 1;
    }

    /** One MGET round trip for the whole seat map, however many seats it has. */
    @Override
    public Map<Long, Hold> holds(Collection<Long> seatIds) {
        if (seatIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = List.copyOf(seatIds);
        List<String> values = redis.opsForValue().multiGet(ids.stream().map(RedisSeatHoldStore::key).toList());
        Map<Long, Hold> holds = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            String value = values == null ? null : values.get(i);
            if (value != null) {
                holds.put(ids.get(i), parse(value));
            }
        }
        return holds;
    }

    static String key(long seatId) {
        return KEY_PREFIX + seatId;
    }

    static Hold parse(String value) {
        int bar = value.lastIndexOf('|');
        return new Hold(value.substring(0, bar), Instant.ofEpochMilli(Long.parseLong(value.substring(bar + 1))));
    }
}
