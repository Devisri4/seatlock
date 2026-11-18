package com.devisri.seatlock.booking;

import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/** A paid booking: one or more seats of one show, bought by one guest. */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "guest_id", nullable = false)
    private String guestId;

    /** Unique in the database: the same key can never create a second booking. */
    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "total_cents", nullable = false)
    private int totalCents;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Rows in booking_seats, whose primary key is seat_id: a seat can be in only one booking. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_seats", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "seat_id")
    private Set<Long> seatIds = new TreeSet<>();

    protected Booking() {
        // for JPA
    }

    public Booking(long showId, String guestId, String idempotencyKey, int totalCents, Instant createdAt, Set<Long> seatIds) {
        this.showId = showId;
        this.guestId = guestId;
        this.idempotencyKey = idempotencyKey;
        this.totalCents = totalCents;
        this.createdAt = createdAt;
        this.seatIds = new TreeSet<>(seatIds);
    }

    public Long getId() {
        return id;
    }

    public Long getShowId() {
        return showId;
    }

    public String getGuestId() {
        return guestId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public int getTotalCents() {
        return totalCents;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Long> getSeatIds() {
        return seatIds;
    }
}
