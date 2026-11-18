package com.devisri.seatlock.show;

/**
 * What the seat map shows for a seat: the database's {@link SeatStatus} combined with the holds in Redis.
 * SOLD wins over HELD; HELD means someone (maybe you) has it for a few minutes.
 */
public enum SeatState {
    AVAILABLE,
    HELD,
    SOLD
}
