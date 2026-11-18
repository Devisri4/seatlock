package com.devisri.seatlock.hold;

import java.time.Instant;

/** A guest's temporary claim on one seat, until {@code expiresAt}. */
public record Hold(String guestId, Instant expiresAt) {

    boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
