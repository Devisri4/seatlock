package com.devisri.seatlock.hold;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** The hold rules on the in-memory store, with a test clock so expiry needs no waiting. */
class InMemorySeatHoldStoreTest extends SeatHoldStoreContract {

    private final MovableClock clock = new MovableClock();

    @Override
    protected SeatHoldStore newStore() {
        return new InMemorySeatHoldStore(clock);
    }

    @Override
    protected void advance(Duration time) {
        clock.now = clock.now.plus(time);
    }

    /** A clock the test can move forward. */
    static class MovableClock extends Clock {
        volatile Instant now = Instant.parse("2030-01-01T00:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
