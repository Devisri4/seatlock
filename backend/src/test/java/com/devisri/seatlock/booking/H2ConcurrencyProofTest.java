package com.devisri.seatlock.booking;

import com.devisri.seatlock.FixedClock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The 500-request proof on H2 + in-memory holds (no Docker needed). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seatlock.demo-data.enabled=false")
@Import(FixedClock.class)
class H2ConcurrencyProofTest extends ConcurrencyProofTest {
}
