package com.devisri.seatlock.hold;

import java.util.List;
import java.util.UUID;

import com.devisri.seatlock.hold.HoldService.HoldView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seat holds. Guests don't sign up: the browser makes a random id once, keeps it, and sends it as
 * {@code X-Guest-Id} on every request. A missing or malformed id is a 400.
 */
@RestController
@RequestMapping("/api/shows/{showId}")
public class HoldController {

    public static final String GUEST_HEADER = "X-Guest-Id";

    private final HoldService service;

    public HoldController(HoldService service) {
        this.service = service;
    }

    /** PUT because holding a seat you already hold is safe to repeat (it just restarts the timer). */
    @PutMapping("/seats/{seatId}/hold")
    public HoldView hold(@PathVariable long showId, @PathVariable long seatId, @RequestHeader(GUEST_HEADER) UUID guestId) {
        return service.hold(showId, seatId, guestId);
    }

    @DeleteMapping("/seats/{seatId}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable long showId, @PathVariable long seatId, @RequestHeader(GUEST_HEADER) UUID guestId) {
        service.release(showId, seatId, guestId);
    }

    @GetMapping("/holds/mine")
    public List<HoldView> myHolds(@PathVariable long showId, @RequestHeader(GUEST_HEADER) UUID guestId) {
        return service.myHolds(showId, guestId);
    }
}
