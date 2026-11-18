package com.devisri.seatlock.live;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.devisri.seatlock.TestData;
import com.devisri.seatlock.live.SeatEvents.SeatUpdate;
import com.devisri.seatlock.show.SeatState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import static com.devisri.seatlock.TestData.newGuest;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two "browsers" watch the same show over real WebSocket connections while guests change seats through
 * the REST API. Holds last 2 seconds here, so the expiry message can be seen without a long wait.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"seatlock.demo-data.enabled=false", "seatlock.holds.duration=2s"})
class LiveSeatMapTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private TestData data;
    private long show;
    private long seat;
    private WebSocketStompClient client;
    private final BlockingQueue<SeatUpdate> browser1 = new LinkedBlockingQueue<>();
    private final BlockingQueue<SeatUpdate> browser2 = new LinkedBlockingQueue<>();

    @BeforeEach
    void openTwoBrowsers() throws Exception {
        data = new TestData(jdbc, rest);
        data.emptyTables();
        show = data.show("Live Show");
        seat = data.seat(show, "A", 1, 3000);

        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new MappingJackson2MessageConverter());
        watch(browser1);
        watch(browser2);
    }

    @AfterEach
    void close() {
        client.stop();
    }

    @Test
    void whenSomeoneHoldsASeatEveryBrowserSeesItTurnHeld() throws Exception {
        data.hold(show, seat, newGuest());

        assertThat(next(browser1)).isEqualTo(new SeatUpdate(seat, SeatState.HELD));
        assertThat(next(browser2)).isEqualTo(new SeatUpdate(seat, SeatState.HELD));
    }

    @Test
    void releasingASeatShowsItAvailableAgain() throws Exception {
        String guest = newGuest();
        data.hold(show, seat, guest);
        data.call(HttpMethod.DELETE, "/api/shows/" + show + "/seats/" + seat + "/hold", guest, null, null);

        assertThat(next(browser1)).isEqualTo(new SeatUpdate(seat, SeatState.HELD));
        assertThat(next(browser1)).isEqualTo(new SeatUpdate(seat, SeatState.AVAILABLE));
    }

    @Test
    void payingShowsTheSeatSoldToEveryone() throws Exception {
        String guest = newGuest();
        data.hold(show, seat, guest);
        data.pay(show, guest, UUID.randomUUID().toString(), seat);

        assertThat(next(browser2)).isEqualTo(new SeatUpdate(seat, SeatState.HELD));
        assertThat(next(browser2)).isEqualTo(new SeatUpdate(seat, SeatState.SOLD));
    }

    @Test
    void aHoldThatRunsOutIsAnnouncedAsAvailable() throws Exception {
        data.hold(show, seat, newGuest());

        assertThat(next(browser1)).isEqualTo(new SeatUpdate(seat, SeatState.HELD));
        assertThat(browser1.poll(5, TimeUnit.SECONDS)).isEqualTo(new SeatUpdate(seat, SeatState.AVAILABLE));
    }

    @Test
    void aFailedHoldSendsNothing() throws Exception {
        data.hold(show, seat, newGuest());
        next(browser1);

        data.hold(show, seat, newGuest()); // 409, someone else has it

        assertThat(browser1.poll(300, TimeUnit.MILLISECONDS)).isNull();
    }

    private SeatUpdate next(BlockingQueue<SeatUpdate> browser) throws InterruptedException {
        SeatUpdate update = browser.poll(3, TimeUnit.SECONDS);
        assertThat(update).as("a live update should arrive").isNotNull();
        return update;
    }

    private void watch(BlockingQueue<SeatUpdate> inbox) throws Exception {
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        }).get(5, TimeUnit.SECONDS);
        StompSession.Receiptable subscription = session.subscribe(SeatEvents.topic(show), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SeatUpdate.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.add((SeatUpdate) payload);
            }
        });
        assertThat(subscription).isNotNull();
        Thread.sleep(150); // let the broker register the subscription before the test acts
    }
}
