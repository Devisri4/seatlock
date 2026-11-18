package com.devisri.seatlock.hold;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param duration how long a seat stays held without paying
 * @param redisUrl Redis address, e.g. Upstash's {@code rediss://default:password@host:6379}. Empty means in-memory holds.
 */
@ConfigurationProperties("seatlock.holds")
public record HoldProperties(@DefaultValue("5m") Duration duration, @DefaultValue("") String redisUrl) {
}
