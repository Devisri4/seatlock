package com.devisri.seatlock.demo;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.devisri.seatlock.show.Show;
import com.devisri.seatlock.show.ShowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty demo with shows. Runs on startup and only adds shows when there are no upcoming ones,
 * so restarting never duplicates data and the live demo never runs out of future shows.
 */
@Component
@ConditionalOnProperty(name = "seatlock.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    /** 8 rows (A front, H back) of 12 seats: 96 seats per show. */
    static final List<String> ROWS = List.of("A", "B", "C", "D", "E", "F", "G", "H");
    static final int SEATS_PER_ROW = 12;

    record DemoShow(String title, String venue, int daysFromToday, LocalTime time) {
    }

    static final List<DemoShow> SHOWS = List.of(
            new DemoShow("Midnight Jazz Session", "Blue Room, Riverside Hall", 1, LocalTime.of(21, 0)),
            new DemoShow("The Last Laugh: Stand-up Night", "Grand Comedy Theatre", 2, LocalTime.of(19, 30)),
            new DemoShow("Symphony Under the Stars", "Open Air Arena", 4, LocalTime.of(18, 30)),
            new DemoShow("Indie Rock Live", "The Warehouse", 7, LocalTime.of(20, 0)));

    private final ShowRepository shows;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ZoneId zone;

    public DemoDataSeeder(ShowRepository shows, JdbcTemplate jdbc, Clock clock,
                          @Value("${seatlock.demo-data.zone:Asia/Kolkata}") ZoneId zone) {
        this.shows = shows;
        this.jdbc = jdbc;
        this.clock = clock;
        this.zone = zone;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (shows.existsByStartsAtAfter(Instant.now(clock))) {
            return;
        }
        LocalDate today = LocalDate.now(clock.withZone(zone));
        for (DemoShow demo : SHOWS) {
            Instant startsAt = today.plusDays(demo.daysFromToday()).atTime(demo.time()).atZone(zone).toInstant();
            Show show = shows.save(new Show(demo.title(), demo.venue(), startsAt));
            addSeats(show.getId());
        }
        log.info("Demo data: added {} shows with {} seats each", SHOWS.size(), ROWS.size() * SEATS_PER_ROW);
    }

    /** One batch insert per show instead of 96 separate round trips (matters on a remote database). */
    private void addSeats(long showId) {
        List<Object[]> seats = new ArrayList<>();
        for (int r = 0; r < ROWS.size(); r++) {
            for (int number = 1; number <= SEATS_PER_ROW; number++) {
                seats.add(new Object[] {showId, ROWS.get(r), number, priceCents(r)});
            }
        }
        jdbc.batchUpdate("insert into seats (show_id, row_label, seat_number, price_cents) values (?, ?, ?, ?)", seats);
    }

    /** Front rows cost more: A-B premium, C-F standard, G-H budget. */
    static int priceCents(int rowIndex) {
        if (rowIndex < 2) {
            return 4500;
        }
        return rowIndex < 6 ? 3000 : 2000;
    }
}
