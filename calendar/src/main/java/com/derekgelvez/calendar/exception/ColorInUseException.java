package com.derekgelvez.calendar.exception;

import lombok.Getter;

/**
 * The colour is already used by another of the user's categories. Returned as 409 Conflict
 * with the id of that category, so the app can ask the user and resend with reassignColor=true.
 */
@Getter
public class ColorInUseException extends RuntimeException {

    private final Long conflictingCategoryId;

    public ColorInUseException(Long conflictingCategoryId) {
        super("Colour is already used by another category");
        this.conflictingCategoryId = conflictingCategoryId;
    }
}
