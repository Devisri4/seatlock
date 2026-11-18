package com.devisri.seatlock.show;

import com.devisri.seatlock.FixedClock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Show API on the in-memory H2 database (no Docker needed). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seatlock.demo-data.enabled=false")
@Import(FixedClock.class)
class H2ShowApiTest extends ShowApiTest {
}
