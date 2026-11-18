package com.devisri.seatlock.hold;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 404: the seat doesn't exist, or belongs to a different show. */
public class SeatNotFoundException extends ErrorResponseException {

    public SeatNotFoundException(long showId, long seatId) {
        super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                "Seat " + seatId + " not found in show " + showId), null);
    }
}
