package com.booking.ticketservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// To return a 400 Bad Request.
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class NotEnoughTicketsException extends RuntimeException {
    public NotEnoughTicketsException(String message) {
        super(message);
    }
}
