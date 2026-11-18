package com.devisri.seatlock.show;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One seat of one show. {@code version} is for optimistic locking: if two transactions try to mark the
 * same seat SOLD, the second one fails instead of silently overwriting the first (used in step 4).
 */
@Entity
@Table(name = "seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "show_id", nullable = false)
    private Long showId;

    @Column(name = "row_label", nullable = false)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Version
    private long version;

    protected Seat() {
        // for JPA
    }

    public Long getId() {
        return id;
    }

    public Long getShowId() {
        return showId;
    }

    public String getRowLabel() {
        return rowLabel;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public String label() {
        return rowLabel + seatNumber;
    }

    /**
     * Marks the seat sold. On commit Hibernate runs {@code UPDATE seats SET status='SOLD', version=v+1
     * WHERE id=? AND version=v}; if another transaction sold it first, no row matches and the commit fails.
     */
    public void markSold() {
        if (status == SeatStatus.SOLD) {
            throw new IllegalStateException("Seat " + label() + " is already sold");
        }
        status = SeatStatus.SOLD;
    }
}
