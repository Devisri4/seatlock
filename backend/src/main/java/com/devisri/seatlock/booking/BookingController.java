package com.devisri.seatlock.booking;

import java.util.List;
import java.util.UUID;

import com.devisri.seatlock.booking.BookingDtos.BookRequest;
import com.devisri.seatlock.booking.BookingDtos.BookingView;
import com.devisri.seatlock.booking.BookingService.BookingResult;
import com.devisri.seatlock.hold.HoldController;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BookingController {

    /** The browser makes one key per checkout and sends it with every retry of the same payment. */
    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    /** Pay for held seats (simulated payment). 201 = new booking, 200 = same key seen before, same booking. */
    @PostMapping("/api/shows/{showId}/bookings")
    public ResponseEntity<BookingView> book(@PathVariable long showId,
                                           @RequestHeader(HoldController.GUEST_HEADER) UUID guestId,
                                           @RequestHeader(IDEMPOTENCY_HEADER) UUID idempotencyKey,
                                           @Valid @RequestBody BookRequest request) {
        BookingResult result = service.book(showId, guestId, idempotencyKey, request.seatIds());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.booking());
    }

    @GetMapping("/api/bookings/mine")
    public List<BookingView> myBookings(@RequestHeader(HoldController.GUEST_HEADER) UUID guestId) {
        return service.myBookings(guestId);
    }
}
