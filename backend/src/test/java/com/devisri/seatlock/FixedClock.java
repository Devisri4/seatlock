package com.devisri.seatlock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Pins "now" to {@link #NOW} in tests that @Import this, so results never depend on today's date. */
@TestConfiguration
public class FixedClock {

    public static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
