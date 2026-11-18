package com.devisri.seatlock.show;

import com.devisri.seatlock.FixedClock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** The same API tests on a real Postgres 16 (what Neon runs). Needs Docker, so it runs in CI. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seatlock.demo-data.enabled=false")
@Import(FixedClock.class)
@Testcontainers(disabledWithoutDocker = true)
class PostgresShowApiTest extends ShowApiTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
