package com.derekgelvez.calendar.exception;

/** Expired, non-existent, already used, or misused invite. Returned as 400 Bad Request. */
public class InviteNotValidException extends RuntimeException {

    public InviteNotValidException(String message) {
        super(message);
    }
}
