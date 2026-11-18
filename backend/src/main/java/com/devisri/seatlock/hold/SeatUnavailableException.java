package com.devisri.seatlock.hold;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 409 Conflict: someone else holds the seat, or it's already sold. */
public class SeatUnavailableException extends ErrorResponseException {

    public SeatUnavailableException(String message) {
        super(HttpStatus.CONFLICT, ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message), null);
    }
}
