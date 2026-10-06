package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.DeleteScope;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Sent after the user confirms deleting an event. For a repeating event the app asks
 * whether to delete just the selected day or the repeating event.
 * occurrenceDate is required unless scope is ALL.
 */
public record DeleteEventDTO(
        @NotNull DeleteScope scope,
        LocalDate occurrenceDate
) {

    @AssertTrue(message = "occurrenceDate is required unless scope is ALL")
    public boolean isOccurrenceDateValid() {
        return scope == null || scope == DeleteScope.ALL || occurrenceDate != null;
    }
}
