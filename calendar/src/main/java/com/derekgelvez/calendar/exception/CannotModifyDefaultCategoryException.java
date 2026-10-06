package com.derekgelvez.calendar.exception;

/** Attempted rename or recolour of the "Uncategorized" category. Returned as 400 Bad Request. */
public class CannotModifyDefaultCategoryException extends RuntimeException {

    public CannotModifyDefaultCategoryException(String message) {
        super(message);
    }
}
