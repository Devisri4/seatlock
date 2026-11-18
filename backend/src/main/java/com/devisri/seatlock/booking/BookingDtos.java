package com.devisri.seatlock.booking;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class BookingDtos {

    private BookingDtos() {
    }

    /** The seats to pay for. They must be held by this guest. */
    public record BookRequest(@NotEmpty @Size(max = 10) List<@NotNull Long> seatIds) {
    }

    /** A confirmed booking, as shown on the ticket and in "My bookings". */
    public record BookingView(Long id, Long showId, String showTitle, String venue, Instant startsAt,
                              List<String> seats, int totalCents, Instant createdAt) {
    }
}
