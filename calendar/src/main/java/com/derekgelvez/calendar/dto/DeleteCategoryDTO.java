package com.derekgelvez.calendar.dto;

import jakarta.validation.constraints.AssertTrue;

/** The category is deleted only if the user chose to delete it and confirmed. */
public record DeleteCategoryDTO(
        @AssertTrue(message = "deletion must be confirmed") boolean confirmed
) {
}
