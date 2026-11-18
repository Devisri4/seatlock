package com.devisri.seatlock.booking;

import com.devisri.seatlock.FixedClock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The 500-request proof on the production setup: real Postgres 16 (Neon) and real Redis 7 (Upstash).
 * Needs Docker, so it runs in CI.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seatlock.demo-data.enabled=false")
@Import(FixedClock.class)
@Testcontainers(disabledWithoutDocker = true)
class PostgresRedisConcurrencyProofTest extends ConcurrencyProofTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void useRedis(DynamicPropertyRegistry registry) {
        registry.add("seatlock.holds.redis-url", () -> "redis://" + REDIS.getHost() + ":" + REDIS.getMappedPort(6379));
    }
}
