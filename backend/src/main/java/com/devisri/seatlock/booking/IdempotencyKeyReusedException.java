package com.devisri.seatlock.booking;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 422: the payment key was already used for a different booking (another guest, show or set of seats). */
public class IdempotencyKeyReusedException extends ErrorResponseException {

    public IdempotencyKeyReusedException() {
        super(HttpStatus.UNPROCESSABLE_ENTITY, ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "This Idempotency-Key was already used for a different booking"), null);
    }
}
