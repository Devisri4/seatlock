package com.devisri.seatlock.show;

/** What the database knows about a seat. Short-lived holds live in Redis, not here (step 3). */
public enum SeatStatus {
    AVAILABLE,
    SOLD
}
