package com.devisri.seatlock.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The rules the database itself enforces, whatever the Java code does.
 * Runs twice: on H2 ({@link H2SchemaTest}, works on any laptop) and on real Postgres ({@link PostgresSchemaTest}, CI).
 * Every test starts from empty tables with one show and two seats.
 */
abstract class SchemaRulesTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void oneShowWithTwoSeats() {
        // H2 does not always roll back a test after a constraint error, so start from empty tables.
        jdbc.update("delete from booking_seats");
        jdbc.update("delete from bookings");
        jdbc.update("delete from seats");
        jdbc.update("delete from shows");
        jdbc.update("insert into shows (id, title, venue, starts_at) values (1, 'Demo Show', 'Hall A', current_timestamp)");
        jdbc.update("insert into seats (id, show_id, row_label, seat_number, price_cents) values (10, 1, 'A', 1, 2500)");
        jdbc.update("insert into seats (id, show_id, row_label, seat_number, price_cents) values (11, 1, 'A', 2, 2500)");
    }

    @Test
    void newSeatsStartAvailableAtVersionZero() {
        assertThat(jdbc.queryForMap("select status, version from seats where id = 10"))
                .containsEntry("status", "AVAILABLE")
                .containsEntry("version", 0L);
    }

    @Test
    void aSeatCanBeSoldOnlyOnce() {
        booking(100, "key-1");
        booking(101, "key-2");
        jdbc.update("insert into booking_seats (booking_id, seat_id) values (100, 10)");

        assertThatThrownBy(() -> jdbc.update("insert into booking_seats (booking_id, seat_id) values (101, 10)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void differentSeatsOfTheSameShowCanBothBeSold() {
        booking(100, "key-1");
        booking(101, "key-2");
        jdbc.update("insert into booking_seats (booking_id, seat_id) values (100, 10)");
        jdbc.update("insert into booking_seats (booking_id, seat_id) values (101, 11)");

        assertThat(jdbc.queryForObject("select count(*) from booking_seats", Integer.class)).isEqualTo(2);
    }

    @Test
    void anIdempotencyKeyCanBeUsedOnlyOnce() {
        booking(100, "same-key");

        assertThatThrownBy(() -> booking(101, "same-key")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aShowCannotHaveTheSameSeatTwice() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into seats (id, show_id, row_label, seat_number, price_cents) values (12, 1, 'A', 1, 2500)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void seatStatusMustBeKnown() {
        assertThatThrownBy(() -> jdbc.update("update seats set status = 'MAYBE' where id = 10"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void booking(long id, String idempotencyKey) {
        jdbc.update("insert into bookings (id, show_id, guest_id, idempotency_key, total_cents, created_at) "
                + "values (?, 1, 'guest-1', ?, 2500, current_timestamp)", id, idempotencyKey);
    }
}
