package com.derekgelvez.calendar.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Create request for an event. For all-day events only the dates of start and end are
 * used, and the end date is inclusive.
 */
public record EventDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String note,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        boolean allDay,
        @NotBlank String timeZone,
        @NotNull Long categoryId,
        @Valid RecurrenceDTO recurrence
) {

    @AssertTrue(message = "end must be after start")
    public boolean isEndAfterStart() {
        return EventValidation.endAfterStart(start, end, allDay);
    }

    @AssertTrue(message = "timeZone must be an IANA time zone such as America/Boise")
    public boolean isTimeZoneValid() {
        return EventValidation.validTimeZone(timeZone);
    }
}
