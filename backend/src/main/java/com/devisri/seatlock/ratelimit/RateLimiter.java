package com.devisri.seatlock.ratelimit;

/**
 * Counts requests per visitor in fixed one-minute windows ("at most N requests in minute 12:04").
 * Production: {@link RedisRateLimiter} (shared by all servers, keys expire by themselves).
 * Local runs without Redis: {@link InMemoryRateLimiter}.
 */
public interface RateLimiter {

    /** Counts this request. {@code retryAfterSeconds} says when the visitor may try again if refused. */
    Decision tryAcquire(String visitor, int limitPerMinute);

    record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
