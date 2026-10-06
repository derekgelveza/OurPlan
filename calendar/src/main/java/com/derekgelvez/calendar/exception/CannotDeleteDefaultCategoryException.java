package com.derekgelvez.calendar.exception;

/** Attempted deletion of the "Uncategorized" category. Returned as 400 Bad Request. */
public class CannotDeleteDefaultCategoryException extends RuntimeException {

    public CannotDeleteDefaultCategoryException(String message) {
        super(message);
    }
}
