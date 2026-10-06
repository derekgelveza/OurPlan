package com.derekgelvez.calendar.exception;

/** A calendar, event, category or user does not exist. Returned as 404 Not Found. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
