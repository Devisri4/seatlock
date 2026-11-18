package com.devisri.seatlock.live;

import com.devisri.seatlock.show.SeatState;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Tells every browser watching a show that one of its seats changed. */
@Component
public class SeatEvents {

    /** One message on the seat map's topic. It says nothing about who holds the seat. */
    public record SeatUpdate(long seatId, SeatState status) {
    }

    private final SimpMessagingTemplate messaging;

    public SeatEvents(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    public void seatChanged(long showId, long seatId, SeatState status) {
        messaging.convertAndSend(topic(showId), new SeatUpdate(seatId, status));
    }

    public static String topic(long showId) {
        return "/topic/shows/" + showId + "/seats";
    }
}
