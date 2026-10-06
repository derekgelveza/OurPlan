package com.derekgelvez.calendar.exception;

/** Duplicate category name for the user. Returned as 409 Conflict. */
public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException(String message) {
        super(message);
    }
}
