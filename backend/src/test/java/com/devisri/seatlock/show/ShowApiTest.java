package com.devisri.seatlock.show;

import java.time.Instant;
import java.util.List;

import com.devisri.seatlock.show.ShowDtos.SeatView;
import com.devisri.seatlock.show.ShowDtos.ShowDetail;
import com.devisri.seatlock.show.ShowDtos.ShowSummary;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.devisri.seatlock.FixedClock.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The read-only show API over HTTP, with its own small data set (demo data is switched off).
 * Runs on H2 ({@link H2ShowApiTest}) and on real Postgres ({@link PostgresShowApiTest}, CI).
 * "Now" is fixed at 2030-01-01T00:00Z.
 */
abstract class ShowApiTest {

    @Autowired
    protected TestRestTemplate rest;

    @Autowired
    protected JdbcTemplate jdbc;

    private long pastShow;
    private long jazz;
    private long comedy;
    private long noSeatsYet;

    @BeforeEach
    void data() {
        jdbc.update("delete from booking_seats");
        jdbc.update("delete from bookings");
        jdbc.update("delete from seats");
        jdbc.update("delete from shows");

        pastShow = show("Yesterday's Show", NOW.minusSeconds(3600));
        comedy = show("Comedy Night", Instant.parse("2030-01-05T14:00:00Z"));
        jazz = show("Jazz Evening", Instant.parse("2030-01-03T15:30:00Z"));
        noSeatsYet = show("Seats Coming Soon", Instant.parse("2030-02-01T12:00:00Z"));

        // Jazz: row B has 2 seats, row A has 3 (inserted out of order on purpose); one seat sold.
        seat(jazz, "B", 2, 2000, "AVAILABLE");
        seat(jazz, "A", 2, 4500, "SOLD");
        seat(jazz, "A", 1, 4500, "AVAILABLE");
        seat(jazz, "B", 1, 2000, "AVAILABLE");
        seat(jazz, "A", 3, 4500, "AVAILABLE");
        seat(comedy, "A", 1, 3000, "AVAILABLE");
        seat(pastShow, "A", 1, 1000, "AVAILABLE");
    }

    @Test
    void listsOnlyUpcomingShowsSoonestFirst() {
        List<ShowSummary> shows = List.of(rest.getForObject("/api/shows", ShowSummary[].class));

        assertThat(shows).extracting(ShowSummary::title)
                .containsExactly("Jazz Evening", "Comedy Night", "Seats Coming Soon");
    }

    @Test
    void eachShowInTheListHasSeatCountsAndTheCheapestPrice() {
        List<ShowSummary> shows = List.of(rest.getForObject("/api/shows", ShowSummary[].class));

        assertThat(shows.get(0)).isEqualTo(new ShowSummary(jazz, "Jazz Evening", "Test Hall",
                Instant.parse("2030-01-03T15:30:00Z"), 5, 4, 2000));
        assertThat(shows.get(1).totalSeats()).isEqualTo(1);
        assertThat(shows.get(2)).extracting(ShowSummary::totalSeats, ShowSummary::availableSeats, ShowSummary::priceFromCents)
                .containsExactly(0L, 0L, 0);
    }

    @Test
    void seatMapGroupsSeatsByRowInOrder() {
        ShowDetail detail = rest.getForObject("/api/shows/" + jazz, ShowDetail.class);

        assertThat(detail.title()).isEqualTo("Jazz Evening");
        assertThat(detail.rows()).extracting(ShowDtos.SeatRow::label).containsExactly("A", "B");
        assertThat(detail.rows().get(0).seats()).extracting(SeatView::number).containsExactly(1, 2, 3);
        assertThat(detail.rows().get(1).seats()).extracting(SeatView::number).containsExactly(1, 2);
    }

    @Test
    void seatMapShowsPriceAndWhetherEachSeatIsSold() {
        ShowDetail detail = rest.getForObject("/api/shows/" + jazz, ShowDetail.class);

        assertThat(detail.rows().get(0).seats()).extracting(SeatView::status)
                .containsExactly(SeatStatus.AVAILABLE, SeatStatus.SOLD, SeatStatus.AVAILABLE);
        assertThat(detail.rows().get(1).seats()).allSatisfy(seat -> assertThat(seat.priceCents()).isEqualTo(2000));
    }

    @Test
    void aShowWithoutSeatsHasAnEmptySeatMap() {
        assertThat(rest.getForObject("/api/shows/" + noSeatsYet, ShowDetail.class).rows()).isEmpty();
    }

    @Test
    void unknownShowIsA404WithAClearMessage() {
        ResponseEntity<JsonNode> response = rest.getForEntity("/api/shows/999999", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(response.getBody().get("detail").asText()).isEqualTo("Show 999999 not found");
    }

    @Test
    void nonNumericShowIdIsA400() {
        assertThat(rest.getForEntity("/api/shows/abc", String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private long show(String title, Instant startsAt) {
        jdbc.update("insert into shows (title, venue, starts_at) values (?, 'Test Hall', ?)",
                title, java.sql.Timestamp.from(startsAt));
        return jdbc.queryForObject("select max(id) from shows", Long.class);
    }

    private void seat(long showId, String row, int number, int priceCents, String status) {
        jdbc.update("insert into seats (show_id, row_label, seat_number, price_cents, status) values (?, ?, ?, ?, ?)",
                showId, row, number, priceCents, status);
    }
}
