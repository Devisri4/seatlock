package com.devisri.seatlock.show;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devisri.seatlock.hold.HoldController;
import com.devisri.seatlock.show.ShowDtos.ShowDetail;
import com.devisri.seatlock.show.ShowDtos.ShowSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shows")
public class ShowController {

    private final ShowService service;

    public ShowController(ShowService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShowSummary> upcomingShows() {
        return service.upcomingShows();
    }

    /** The guest id is optional here: with it, the seats you hold are marked {@code heldByYou}. */
    @GetMapping("/{showId}")
    public ShowDetail show(@PathVariable long showId,
                           @RequestHeader(value = HoldController.GUEST_HEADER, required = false) UUID guestId) {
        return service.show(showId, Optional.ofNullable(guestId));
    }
}
