package com.devisri.seatlock.schema;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;

/** Schema rules on the in-memory H2 database from application.properties (no Docker needed). */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class H2SchemaTest extends SchemaRulesTest {
}
