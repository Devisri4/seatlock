package com.devisri.seatlock.hold;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules every {@link SeatHoldStore} must follow. Runs against the in-memory store
 * ({@link InMemorySeatHoldStoreTest}) and against real Redis ({@link RedisSeatHoldStoreTest}, CI).
 */
abstract class SeatHoldStoreContract {

    static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
    static final Duration SHORT = Duration.ofMillis(800);

    private final String alice = UUID.randomUUID().toString();
    private final String bob = UUID.randomUUID().toString();
    private long seat;

    protected SeatHoldStore store;

    protected abstract SeatHoldStore newStore();

    /** Lets time pass: moves a test clock, or really waits for Redis. */
    protected abstract void advance(Duration time) throws InterruptedException;

    @BeforeEach
    void setUp() {
        store = newStore();
        seat = 1_000 + (long) (Math.random() * 1_000_000); // a fresh seat per test, even on a shared Redis
    }

    @Test
    void aFreeSeatCanBeHeld() {
        Optional<Hold> hold = store.hold(seat, alice, FIVE_MINUTES);

        assertThat(hold).hasValueSatisfying(h -> assertThat(h.guestId()).isEqualTo(alice));
        assertThat(store.holds(List.of(seat))).containsOnlyKeys(seat);
    }

    @Test
    void someoneElseCannotHoldTheSameSeat() {
        store.hold(seat, alice, FIVE_MINUTES);

        assertThat(store.hold(seat, bob, FIVE_MINUTES)).isEmpty();
        assertThat(store.holds(List.of(seat)).get(seat).guestId()).isEqualTo(alice);
    }

    @Test
    void holdingYourOwnSeatAgainRestartsTheTimer() throws InterruptedException {
        Hold first = store.hold(seat, alice, FIVE_MINUTES).orElseThrow();
        advance(Duration.ofMillis(20));

        Hold again = store.hold(seat, alice, FIVE_MINUTES).orElseThrow();

        assertThat(again.expiresAt()).isAfter(first.expiresAt());
        assertThat(store.holds(List.of(seat)).get(seat).expiresAt()).isEqualTo(again.expiresAt());
    }

    @Test
    void aHoldExpiresByItselfAndTheSeatIsFreeAgain() throws InterruptedException {
        store.hold(seat, alice, SHORT);

        advance(SHORT.plusMillis(400));

        assertThat(store.holds(List.of(seat))).isEmpty();
        assertThat(store.hold(seat, bob, FIVE_MINUTES)).isPresent();
    }

    @Test
    void releasingYourHoldFreesTheSeat() {
        store.hold(seat, alice, FIVE_MINUTES);

        assertThat(store.release(seat, alice)).isTrue();
        assertThat(store.holds(List.of(seat))).isEmpty();
        assertThat(store.hold(seat, bob, FIVE_MINUTES)).isPresent();
    }

    @Test
    void youCannotReleaseSomeoneElsesHold() {
        store.hold(seat, alice, FIVE_MINUTES);

        assertThat(store.release(seat, bob)).isFalse();
        assertThat(store.holds(List.of(seat)).get(seat).guestId()).isEqualTo(alice);
    }

    @Test
    void releasingASeatNobodyHoldsDoesNothing() {
        assertThat(store.release(seat, alice)).isFalse();
    }

    @Test
    void holdsForManySeatsComeBackInOneCallWithFreeSeatsLeftOut() {
        store.hold(seat, alice, FIVE_MINUTES);
        store.hold(seat + 1, bob, FIVE_MINUTES);

        Map<Long, Hold> holds = store.holds(List.of(seat, seat + 1, seat + 2));

        assertThat(holds).containsOnlyKeys(seat, seat + 1);
        assertThat(holds.get(seat + 1).guestId()).isEqualTo(bob);
        assertThat(store.holds(List.of())).isEmpty();
    }

    @Test
    void when50GuestsClickTheSameSeatAtOnceExactlyOneGetsIt() throws Exception {
        int guests = 50;
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(guests);
        try {
            List<Callable<Boolean>> clicks = IntStream.range(0, guests).<Callable<Boolean>>mapToObj(i -> () -> {
                go.await(); // everyone waits here, then all click together
                return store.hold(seat, UUID.randomUUID().toString(), FIVE_MINUTES).isPresent();
            }).toList();
            List<Future<Boolean>> results = clicks.stream().map(pool::submit).toList();
            go.countDown();

            long winners = 0;
            for (Future<Boolean> result : results) {
                winners += result.get() ? 1 : 0;
            }
            assertThat(winners).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }
}
