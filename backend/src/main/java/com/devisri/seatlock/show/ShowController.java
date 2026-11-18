package com.devisri.seatlock.show;

import java.util.List;

import com.devisri.seatlock.show.ShowDtos.ShowDetail;
import com.devisri.seatlock.show.ShowDtos.ShowSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/{showId}")
    public ShowDetail show(@PathVariable long showId) {
        return service.show(showId);
    }
}
