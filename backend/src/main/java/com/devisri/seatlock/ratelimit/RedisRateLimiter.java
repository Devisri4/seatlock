package com.devisri.seatlock.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * One Redis counter per visitor per minute ({@code seatlock:rate:<ip>:<minute>}). INCR is atomic, so
 * parallel requests are all counted; the counter is given an expiry when created and disappears by itself.
 */
public class RedisRateLimiter implements RateLimiter {

    static final String KEY_PREFIX = "seatlock:rate:";
    private static final long WINDOW_MS = 60_000;

    /** KEYS[1] counter, ARGV[1] expiry in ms. Count the request; start the expiry on the first one. Returns the count. */
    private static final RedisScript<Long> COUNT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redis;
    private final Clock clock;

    public RedisRateLimiter(StringRedisTemplate redis, Clock clock) {
        this.redis = redis;
        this.clock = clock;
    }

    @Override
    public Decision tryAcquire(String visitor, int limitPerMinute) {
        long now = clock.millis();
        long window = now / WINDOW_MS;
        long msLeft = (window + 1) * WINDOW_MS - now;
        Long count = redis.execute(COUNT, List.of(KEY_PREFIX + visitor + ":" + window), String.valueOf(msLeft + 1000));
        boolean allowed = count != null && count <= limitPerMinute;
        return new Decision(allowed, allowed ? 0 : Math.max(1, (msLeft + 999) / 1000));
    }
}
