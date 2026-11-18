package com.devisri.seatlock.show;

import java.time.Instant;
import java.util.List;

/** What the API sends back. Records keep the JSON shape separate from the database entities. */
public final class ShowDtos {

    private ShowDtos() {
    }

    /** One card in the show list. */
    public record ShowSummary(Long id, String title, String venue, Instant startsAt,
                              long totalSeats, long availableSeats, int priceFromCents) {
    }

    /** One show with its full seat map, row by row (A, B, C...). */
    public record ShowDetail(Long id, String title, String venue, Instant startsAt, List<SeatRow> rows) {
    }

    public record SeatRow(String label, List<SeatView> seats) {
    }

    public record SeatView(Long id, int number, int priceCents, SeatStatus status) {
    }
}
