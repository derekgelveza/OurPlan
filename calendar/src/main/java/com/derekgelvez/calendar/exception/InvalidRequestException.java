package com.derekgelvez.calendar.exception;

/** A request that passes field validation but breaks a business rule. Returned as 400 Bad Request. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
