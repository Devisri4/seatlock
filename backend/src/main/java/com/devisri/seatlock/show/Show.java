package com.devisri.seatlock.show;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One performance of an event at a venue and time. */
@Entity
@Table(name = "shows")
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String venue;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    protected Show() {
        // for JPA
    }

    public Show(String title, String venue, Instant startsAt) {
        this.title = title;
        this.venue = venue;
        this.startsAt = startsAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getVenue() {
        return venue;
    }

    public Instant getStartsAt() {
        return startsAt;
    }
}
