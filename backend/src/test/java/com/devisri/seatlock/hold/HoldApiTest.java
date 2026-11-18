package com.devisri.seatlock.hold;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devisri.seatlock.FixedClock;
import com.devisri.seatlock.show.SeatState;
import com.devisri.seatlock.show.ShowDtos.SeatView;
import com.devisri.seatlock.show.ShowDtos.ShowDetail;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.devisri.seatlock.FixedClock.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/** Holding and releasing seats over HTTP (H2 + in-memory holds). "Now" is fixed at 2030-01-01T00:00Z. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seatlock.demo-data.enabled=false")
@Import(FixedClock.class)
class HoldApiTest {

    private static final String ALICE = UUID.randomUUID().toString();
    private static final String BOB = UUID.randomUUID().toString();

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private long show;
    private long otherShow;
    private long a1;
    private long a2;
    private long soldSeat;
    private long otherShowSeat;

    @BeforeEach
    void data() {
        jdbc.update("delete from booking_seats");
        jdbc.update("delete from bookings");
        jdbc.update("delete from seats");
        jdbc.update("delete from shows");
        show = insertShow("Jazz Evening");
        otherShow = insertShow("Comedy Night");
        a1 = insertSeat(show, 1, "AVAILABLE");
        a2 = insertSeat(show, 2, "AVAILABLE");
        soldSeat = insertSeat(show, 3, "SOLD");
        otherShowSeat = insertSeat(otherShow, 1, "AVAILABLE");
    }

    @Test
    void holdingAFreeSeatReturnsWhenTheHoldEnds() {
        ResponseEntity<JsonNode> response = hold(a1, ALICE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("seatId").asLong()).isEqualTo(a1);
        assertThat(Instant.parse(response.getBody().get("expiresAt").asText())).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void aSeatSomeoneElseHoldsIsA409() {
        hold(a1, ALICE);

        ResponseEntity<JsonNode> response = hold(a1, BOB);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("detail").asText()).isEqualTo("Seat A1 is held by someone else");
    }

    @Test
    void holdingYourOwnSeatAgainIsFine() {
        hold(a1, ALICE);

        assertThat(hold(a1, ALICE).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void aSoldSeatCannotBeHeld() {
        ResponseEntity<JsonNode> response = hold(soldSeat, ALICE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("detail").asText()).isEqualTo("Seat A3 is already sold");
    }

    @Test
    void aSeatOfAnotherShowIsA404() {
        ResponseEntity<JsonNode> response = call(HttpMethod.PUT, "/api/shows/" + show + "/seats/" + otherShowSeat + "/hold", ALICE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("detail").asText()).isEqualTo("Seat " + otherShowSeat + " not found in show " + show);
    }

    @Test
    void aGuestIdIsRequiredAndMustBeAUuid() {
        assertThat(call(HttpMethod.PUT, holdUrl(a1), null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(call(HttpMethod.PUT, holdUrl(a1), "not-a-uuid").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void theSeatMapShowsHeldSeatsToEveryoneButMarksOnlyYourOwn() {
        hold(a1, ALICE);

        SeatView forAlice = seatMap(ALICE).get(0);
        SeatView forBob = seatMap(BOB).get(0);
        SeatView forAnonymous = seatMap(null).get(0);

        assertThat(forAlice.status()).isEqualTo(SeatState.HELD);
        assertThat(forAlice.heldByYou()).isTrue();
        assertThat(forBob.status()).isEqualTo(SeatState.HELD);
        assertThat(forBob.heldByYou()).isFalse();
        assertThat(forAnonymous.heldByYou()).isFalse();
        assertThat(seatMap(BOB)).extracting(SeatView::status)
                .containsExactly(SeatState.HELD, SeatState.AVAILABLE, SeatState.SOLD);
    }

    @Test
    void myHoldsListsOnlyMySeats() {
        hold(a1, ALICE);
        hold(a2, BOB);

        JsonNode mine = call(HttpMethod.GET, "/api/shows/" + show + "/holds/mine", ALICE).getBody();

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).get("seatId").asLong()).isEqualTo(a1);
    }

    @Test
    void releasingMakesTheSeatAvailableForOthers() {
        hold(a1, ALICE);

        assertThat(call(HttpMethod.DELETE, holdUrl(a1), ALICE).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(seatMap(BOB).get(0).status()).isEqualTo(SeatState.AVAILABLE);
        assertThat(hold(a1, BOB).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void releasingSomeoneElsesHoldLeavesItHeld() {
        hold(a1, ALICE);

        assertThat(call(HttpMethod.DELETE, holdUrl(a1), BOB).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(seatMap(ALICE).get(0).heldByYou()).isTrue();
    }

    private ResponseEntity<JsonNode> hold(long seatId, String guest) {
        return call(HttpMethod.PUT, holdUrl(seatId), guest);
    }

    private String holdUrl(long seatId) {
        return "/api/shows/" + show + "/seats/" + seatId + "/hold";
    }

    private List<SeatView> seatMap(String guest) {
        HttpHeaders headers = new HttpHeaders();
        if (guest != null) {
            headers.set(HoldController.GUEST_HEADER, guest);
        }
        ShowDetail detail = rest.exchange("/api/shows/" + show, HttpMethod.GET, new HttpEntity<>(headers), ShowDetail.class).getBody();
        return detail.rows().get(0).seats();
    }

    private ResponseEntity<JsonNode> call(HttpMethod method, String url, String guest) {
        HttpHeaders headers = new HttpHeaders();
        if (guest != null) {
            headers.set(HoldController.GUEST_HEADER, guest);
        }
        return rest.exchange(url, method, new HttpEntity<>(headers), JsonNode.class);
    }

    private long insertShow(String title) {
        jdbc.update("insert into shows (title, venue, starts_at) values (?, 'Test Hall', ?)",
                title, java.sql.Timestamp.from(NOW.plus(Duration.ofDays(3))));
        return jdbc.queryForObject("select max(id) from shows", Long.class);
    }

    private long insertSeat(long showId, int number, String status) {
        jdbc.update("insert into seats (show_id, row_label, seat_number, price_cents, status) values (?, 'A', ?, 3000, ?)",
                showId, number, status);
        return jdbc.queryForObject("select max(id) from seats", Long.class);
    }
}
