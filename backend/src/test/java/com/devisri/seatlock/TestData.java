package com.devisri.seatlock;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.UUID;

import com.devisri.seatlock.booking.BookingController;
import com.devisri.seatlock.hold.HoldController;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/** Small helpers shared by the HTTP tests: fresh tables, a show with seats, and API calls as a guest. */
public final class TestData {

    private final JdbcTemplate jdbc;
    private final TestRestTemplate rest;

    public TestData(JdbcTemplate jdbc, TestRestTemplate rest) {
        this.jdbc = jdbc;
        this.rest = rest;
    }

    public void emptyTables() {
        jdbc.update("delete from booking_seats");
        jdbc.update("delete from bookings");
        jdbc.update("delete from seats");
        jdbc.update("delete from shows");
    }

    public long show(String title) {
        jdbc.update("insert into shows (title, venue, starts_at) values (?, 'Test Hall', ?)",
                title, Timestamp.from(FixedClock.NOW.plus(Duration.ofDays(3))));
        return jdbc.queryForObject("select max(id) from shows", Long.class);
    }

    public long seat(long showId, String row, int number, int priceCents) {
        jdbc.update("insert into seats (show_id, row_label, seat_number, price_cents) values (?, ?, ?, ?)",
                showId, row, number, priceCents);
        return jdbc.queryForObject("select max(id) from seats", Long.class);
    }

    public static String newGuest() {
        return UUID.randomUUID().toString();
    }

    public ResponseEntity<JsonNode> hold(long showId, long seatId, String guest) {
        return call(HttpMethod.PUT, "/api/shows/" + showId + "/seats/" + seatId + "/hold", guest, null, null);
    }

    public ResponseEntity<JsonNode> pay(long showId, String guest, String idempotencyKey, long... seatIds) {
        StringBuilder body = new StringBuilder("{\"seatIds\":[");
        for (int i = 0; i < seatIds.length; i++) {
            body.append(i == 0 ? "" : ",").append(seatIds[i]);
        }
        return call(HttpMethod.POST, "/api/shows/" + showId + "/bookings", guest, idempotencyKey, body.append("]}").toString());
    }

    public ResponseEntity<JsonNode> call(HttpMethod method, String url, String guest, String idempotencyKey, String json) {
        HttpHeaders headers = new HttpHeaders();
        if (guest != null) {
            headers.set(HoldController.GUEST_HEADER, guest);
        }
        if (idempotencyKey != null) {
            headers.set(BookingController.IDEMPOTENCY_HEADER, idempotencyKey);
        }
        if (json != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        return rest.exchange(url, method, new HttpEntity<>(json, headers), JsonNode.class);
    }

    public int count(String sql, Object... args) {
        return jdbc.queryForObject(sql, Integer.class, args);
    }
}
