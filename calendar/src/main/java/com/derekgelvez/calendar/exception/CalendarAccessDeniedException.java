package com.derekgelvez.calendar.exception;

/** Insufficient access level or invalid event creator permissions. Returned as 403 Forbidden. */
public class CalendarAccessDeniedException extends RuntimeException {

    public CalendarAccessDeniedException(String message) {
        super(message);
    }
}
