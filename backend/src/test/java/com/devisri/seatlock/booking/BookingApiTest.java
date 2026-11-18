package com.devisri.seatlock.booking;

import java.util.UUID;

import com.devisri.seatlock.FixedClock;
import com.devisri.seatlock.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.devisri.seatlock.TestData.newGuest;
import static org.assertj.core.api.Assertions.assertThat;

/** Paying for held seats over HTTP (H2 + in-memory holds). "Now" is fixed at 2030-01-01T00:00Z. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seatlock.demo-data.enabled=false")
@Import(FixedClock.class)
class BookingApiTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private TestData data;
    private long show;
    private long a1;
    private long a2;
    private final String alice = newGuest();
    private final String bob = newGuest();

    @BeforeEach
    void setUp() {
        data = new TestData(jdbc, rest);
        data.emptyTables();
        show = data.show("Jazz Evening");
        a1 = data.seat(show, "A", 1, 4500);
        a2 = data.seat(show, "A", 2, 3000);
    }

    @Test
    void payingForHeldSeatsCreatesABookingAndSellsTheSeats() {
        data.hold(show, a1, alice);
        data.hold(show, a2, alice);

        ResponseEntity<JsonNode> response = data.pay(show, alice, key(), a2, a1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode booking = response.getBody();
        assertThat(booking.get("showTitle").asText()).isEqualTo("Jazz Evening");
        assertThat(booking.get("seats")).extracting(JsonNode::asText).containsExactly("A1", "A2");
        assertThat(booking.get("totalCents").asInt()).isEqualTo(7500);
        assertThat(data.count("select count(*) from seats where status = 'SOLD'")).isEqualTo(2);
    }

    @Test
    void afterPayingTheSeatsAreSoldOnTheSeatMapAndTheHoldsAreGone() {
        data.hold(show, a1, alice);
        data.pay(show, alice, key(), a1);

        JsonNode seat = data.call(HttpMethod.GET, "/api/shows/" + show, alice, null, null).getBody()
                .get("rows").get(0).get("seats").get(0);
        assertThat(seat.get("status").asText()).isEqualTo("SOLD");
        assertThat(seat.get("heldByYou").asBoolean()).isFalse();
        assertThat(data.call(HttpMethod.GET, "/api/shows/" + show + "/holds/mine", alice, null, null).getBody()).isEmpty();
    }

    @Test
    void payingTwiceWithTheSameKeyReturnsTheSameBookingAndChargesOnce() {
        data.hold(show, a1, alice);
        String key = key();

        ResponseEntity<JsonNode> first = data.pay(show, alice, key, a1);
        ResponseEntity<JsonNode> second = data.pay(show, alice, key, a1);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getBody().get("id")).isEqualTo(first.getBody().get("id"));
        assertThat(data.count("select count(*) from bookings")).isEqualTo(1);
    }

    @Test
    void reusingAKeyForDifferentSeatsIsA422() {
        data.hold(show, a1, alice);
        data.hold(show, a2, alice);
        String key = key();
        data.pay(show, alice, key, a1);

        ResponseEntity<JsonNode> response = data.pay(show, alice, key, a2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(data.count("select count(*) from bookings")).isEqualTo(1);
    }

    @Test
    void youCannotPayForASeatYouDoNotHold() {
        ResponseEntity<JsonNode> response = data.pay(show, alice, key(), a1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("detail").asText()).isEqualTo("Seat A1 is not held by you (the hold may have expired)");
    }

    @Test
    void youCannotPayForASeatSomeoneElseHolds() {
        data.hold(show, a1, bob);

        assertThat(data.pay(show, alice, key(), a1).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(data.count("select count(*) from bookings")).isZero();
    }

    @Test
    void aSoldSeatCannotBeBoughtAgain() {
        data.hold(show, a1, alice);
        data.pay(show, alice, key(), a1);

        ResponseEntity<JsonNode> response = data.pay(show, bob, key(), a1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("detail").asText()).isEqualTo("Seat A1 is already sold");
    }

    @Test
    void aSeatOfAnotherShowIsA404() {
        long other = data.show("Other");
        long otherSeat = data.seat(other, "A", 1, 1000);

        assertThat(data.pay(show, alice, key(), otherSeat).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void badRequestsAre400() {
        String url = "/api/shows/" + show + "/bookings";
        assertThat(data.call(HttpMethod.POST, url, alice, key(), "{\"seatIds\":[]}").getStatusCode())
                .as("no seats").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(data.call(HttpMethod.POST, url, alice, null, "{\"seatIds\":[" + a1 + "]}").getStatusCode())
                .as("no Idempotency-Key").isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(data.call(HttpMethod.POST, url, null, key(), "{\"seatIds\":[" + a1 + "]}").getStatusCode())
                .as("no guest id").isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void myBookingsListsOnlyMyBookingsNewestFirst() {
        data.hold(show, a1, alice);
        data.hold(show, a2, bob);
        data.pay(show, alice, key(), a1);
        data.pay(show, bob, key(), a2);

        JsonNode mine = data.call(HttpMethod.GET, "/api/bookings/mine", alice, null, null).getBody();

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("seats").get(0).asText()).isEqualTo("A1");
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
