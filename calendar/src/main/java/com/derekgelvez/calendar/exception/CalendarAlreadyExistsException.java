package com.derekgelvez.calendar.exception;

/** The user already has a calendar. Returned as 409 Conflict. */
public class CalendarAlreadyExistsException extends RuntimeException {

    public CalendarAlreadyExistsException(String message) {
        super(message);
    }
}
