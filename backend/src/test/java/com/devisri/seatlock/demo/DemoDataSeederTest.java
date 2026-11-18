package com.devisri.seatlock.demo;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.devisri.seatlock.FixedClock;
import com.devisri.seatlock.show.ShowRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import static com.devisri.seatlock.FixedClock.NOW;
import static org.assertj.core.api.Assertions.assertThat;

/** The seeder already ran once on startup (demo data is on by default). "Now" is 2030-01-01T00:00Z. */
@SpringBootTest
@Import(FixedClock.class)
@DirtiesContext
class DemoDataSeederTest {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private ShowRepository shows;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void startupAddsFourShowsOf96Seats() {
        assertThat(shows.count()).isEqualTo(4);
        assertThat(jdbc.queryForList("select count(*) from seats group by show_id", Long.class))
                .containsExactly(96L, 96L, 96L, 96L);
    }

    @Test
    void showsStartAtLocalEveningTimesInTheComingWeek() {
        List<Instant> times = jdbc.queryForList("select starts_at from shows order by starts_at", java.sql.Timestamp.class)
                .stream().map(java.sql.Timestamp::toInstant).toList();

        // Clock is 2030-01-01 05:30 in India, so "tomorrow 21:00" is 2030-01-02 21:00 IST.
        assertThat(times.get(0)).isEqualTo(java.time.LocalDateTime.parse("2030-01-02T21:00").atZone(INDIA).toInstant());
        assertThat(times).allSatisfy(t -> assertThat(t).isBetween(NOW, NOW.plus(Duration.ofDays(8))));
    }

    @Test
    void frontRowsCostMoreThanBackRows() {
        assertThat(jdbc.queryForObject("select min(price_cents) from seats where row_label = 'A'", Integer.class)).isEqualTo(4500);
        assertThat(jdbc.queryForObject("select max(price_cents) from seats where row_label = 'H'", Integer.class)).isEqualTo(2000);
    }

    @Test
    void runningAgainWhileShowsAreUpcomingAddsNothing() {
        seeder.run(null);

        assertThat(shows.count()).isEqualTo(4);
    }

    @Test
    @DirtiesContext // adds rows; the next test gets a fresh database
    void onceAllShowsHavePassedANewWeekOfShowsIsAdded() {
        Clock twoWeeksLater = Clock.fixed(NOW.plus(Duration.ofDays(14)), ZoneOffset.UTC);

        new DemoDataSeeder(shows, jdbc, twoWeeksLater, INDIA).run(null);

        assertThat(shows.count()).isEqualTo(8);
        assertThat(shows.existsByStartsAtAfter(twoWeeksLater.instant())).isTrue();
    }
}
