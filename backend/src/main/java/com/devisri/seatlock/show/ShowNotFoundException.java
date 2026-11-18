package com.devisri.seatlock.show;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** Becomes a 404 with a standard JSON problem body ({@code {"status":404,"detail":"Show 42 not found",...}}). */
public class ShowNotFoundException extends ErrorResponseException {

    public ShowNotFoundException(long showId) {
        super(HttpStatus.NOT_FOUND, ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "Show " + showId + " not found"), null);
    }
}
