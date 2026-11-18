package com.devisri.seatlock.booking;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import com.devisri.seatlock.TestData;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static com.devisri.seatlock.TestData.newGuest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The proof behind SeatLock: 500 requests hit one seat at the same moment, over real HTTP, and exactly one
 * booking comes out. Each test attacks a different line of defence:
 * <ol>
 *   <li>500 guests click and pay for the same seat: the atomic hold lets only one through.</li>
 *   <li>The holder fires 500 payments with different keys (holds can't help): optimistic locking +
 *       the booking_seats primary key sell the seat once.</li>
 *   <li>The holder retries one payment 500 times with the same key: the unique idempotency key
 *       makes every retry return the same booking.</li>
 * </ol>
 * Runs on H2 + in-memory holds ({@link H2ConcurrencyProofTest}) and on Postgres + Redis
 * ({@link PostgresRedisConcurrencyProofTest}, CI).
 */
abstract class ConcurrencyProofTest {

    static final int REQUESTS = 500;

    private static final Logger log = LoggerFactory.getLogger(ConcurrencyProofTest.class);

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private TestData data;
    private long show;
    private long seat;

    @BeforeEach
    void oneShowWithOneSeat() {
        data = new TestData(jdbc, rest);
        data.emptyTables();
        show = data.show("Sold Out Concert");
        seat = data.seat(show, "A", 1, 5000);
    }

    @Test
    void fiveHundredGuestsRaceForOneSeatAndExactlyOneBooksIt() throws Exception {
        List<String> outcomes = race(i -> () -> {
            String guest = newGuest();
            ResponseEntity<JsonNode> hold = data.hold(show, seat, guest);
            if (hold.getStatusCode() != HttpStatus.OK) {
                return "hold " + hold.getStatusCode().value();
            }
            return "pay " + data.pay(show, guest, UUID.randomUUID().toString(), seat).getStatusCode().value();
        });

        assertThat(count(outcomes)).containsExactlyInAnyOrderEntriesOf(Map.of("pay 201", 1L, "hold 409", 499L));
        assertExactlyOneBooking();
    }

    @Test
    void evenIfTheHoldLetsEveryRequestThroughTheDatabaseSellsTheSeatOnce() throws Exception {
        String guest = newGuest();
        data.hold(show, seat, guest);

        List<String> outcomes = race(i -> () ->
                "pay " + data.pay(show, guest, UUID.randomUUID().toString(), seat).getStatusCode().value());

        assertThat(count(outcomes)).containsExactlyInAnyOrderEntriesOf(Map.of("pay 201", 1L, "pay 409", 499L));
        assertExactlyOneBooking();
    }

    @Test
    void fiveHundredRetriesOfOnePaymentCreateOneBookingAndAllGetIt() throws Exception {
        String guest = newGuest();
        String key = UUID.randomUUID().toString();
        data.hold(show, seat, guest);

        List<String> outcomes = race(i -> () -> {
            ResponseEntity<JsonNode> paid = data.pay(show, guest, key, seat);
            return "pay " + paid.getStatusCode().value() + " booking " + paid.getBody().get("id").asLong();
        });

        long bookingId = jdbc.queryForObject("select id from bookings", Long.class);
        assertThat(count(outcomes)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "pay 201 booking " + bookingId, 1L,
                "pay 200 booking " + bookingId, (long) REQUESTS - 1));
        assertExactlyOneBooking();
    }

    private void assertExactlyOneBooking() {
        assertThat(data.count("select count(*) from bookings")).isEqualTo(1);
        assertThat(data.count("select count(*) from booking_seats where seat_id = ?", seat)).isEqualTo(1);
        assertThat(data.count("select count(*) from seats where id = ? and status = 'SOLD'", seat)).isEqualTo(1);
    }

    /** Starts all requests on their own (virtual) threads, releases them at the same instant, and collects the outcomes. */
    private List<String> race(IntFunction<Callable<String>> request) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService threads = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                Callable<String> call = request.apply(i);
                futures.add(threads.submit(() -> {
                    start.await();
                    return call.call();
                }));
            }
            long began = System.nanoTime();
            start.countDown();
            List<String> outcomes = new ArrayList<>();
            for (Future<String> future : futures) {
                outcomes.add(future.get());
            }
            log.info("{} parallel requests in {} ms -> {}", REQUESTS, (System.nanoTime() - began) / 1_000_000, count(outcomes));
            return outcomes;
        }
    }

    private static Map<String, Long> count(List<String> outcomes) {
        return outcomes.stream().collect(Collectors.groupingBy(o -> o, Collectors.counting()));
    }
}
