package com.devisri.seatlock.ratelimit;

import java.time.Clock;

class InMemoryRateLimiterTest extends RateLimiterContract {

    /** One limiter shared by the clocks of a test, like one running server. */
    private final MutableClockLimiter shared = new MutableClockLimiter();

    @Override
    protected RateLimiter limiter(Clock clock) {
        shared.clock = clock;
        return shared;
    }

    /** Lets a test move the shared limiter to another minute. */
    static class MutableClockLimiter implements RateLimiter {
        Clock clock;
        private final InMemoryRateLimiter limiter = new InMemoryRateLimiter(new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return clock.getZone();
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public java.time.Instant instant() {
                return clock.instant();
            }
        });

        @Override
        public Decision tryAcquire(String visitor, int limitPerMinute) {
            return limiter.tryAcquire(visitor, limitPerMinute);
        }
    }
}
