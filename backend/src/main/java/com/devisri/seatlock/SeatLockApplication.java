package com.devisri.seatlock;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SeatLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeatLockApplication.class, args);
    }

    /** One clock for "now", so tests can pin the time instead of depending on the real date. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
