package com.devisri.seatlock.ratelimit;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The counting rules every {@link RateLimiter} follows: in memory and on real Redis ({@link RedisRateLimiterTest}). */
abstract class RateLimiterContract {

    /** 20 seconds into a minute, so 40 seconds are left. */
    static final Clock CLOCK = Clock.fixed(Instant.parse("2030-01-01T00:00:20Z"), ZoneOffset.UTC);

    protected abstract RateLimiter limiter(Clock clock);

    private final String visitor = "203.0.113." + UUID.randomUUID(); // fresh per test, even on a shared Redis

    @Test
    void allowsUpToTheLimitThenRefusesUntilTheNextMinute() {
        RateLimiter limiter = limiter(CLOCK);

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire(visitor, 5).allowed()).isTrue();
        }
        RateLimiter.Decision refused = limiter.tryAcquire(visitor, 5);

        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds()).isEqualTo(40);
    }

    @Test
    void aNewMinuteStartsANewCount() {
        RateLimiter now = limiter(CLOCK);
        now.tryAcquire(visitor, 1);
        assertThat(now.tryAcquire(visitor, 1).allowed()).isFalse();

        RateLimiter nextMinute = limiter(Clock.offset(CLOCK, java.time.Duration.ofMinutes(1)));

        assertThat(nextMinute.tryAcquire(visitor, 1).allowed()).isTrue();
    }

    @Test
    void visitorsAreCountedSeparately() {
        RateLimiter limiter = limiter(CLOCK);
        limiter.tryAcquire(visitor, 1);

        assertThat(limiter.tryAcquire(visitor + "-other", 1).allowed()).isTrue();
    }
}
