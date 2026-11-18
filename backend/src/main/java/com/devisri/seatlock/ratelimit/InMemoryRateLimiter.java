package com.devisri.seatlock.ratelimit;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** Same rules as {@link RedisRateLimiter}, in this server's memory (local runs, tests). */
public class InMemoryRateLimiter implements RateLimiter {

    private static final long WINDOW_MS = 60_000;

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final AtomicLong currentWindow = new AtomicLong(-1);
    private final Clock clock;

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public Decision tryAcquire(String visitor, int limitPerMinute) {
        long now = clock.millis();
        long window = now / WINDOW_MS;
        if (currentWindow.getAndSet(window) != window) {
            counters.clear(); // a new minute: forget last minute's counts (Redis expires them instead)
        }
        int count = counters.computeIfAbsent(visitor, v -> new AtomicInteger()).incrementAndGet();
        boolean allowed = count <= limitPerMinute;
        long msLeft = (window + 1) * WINDOW_MS - now;
        return new Decision(allowed, allowed ? 0 : Math.max(1, (msLeft + 999) / 1000));
    }
}
