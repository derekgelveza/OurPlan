package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.DeleteScope;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Same fields and rules as {@link EventDTO}. For a repeating event it also carries the
 * scope of the edit and the selected occurrenceDate (required unless scope is ALL).
 */
public record UpdateEventDTO(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 1000) String note,
        @NotNull LocalDateTime start,
        @NotNull LocalDateTime end,
        boolean allDay,
        @NotBlank String timeZone,
        @NotNull Long categoryId,
        @Valid RecurrenceDTO recurrence,
        DeleteScope scope,
        LocalDate occurrenceDate
) {

    @AssertTrue(message = "end must be after start")
    public boolean isEndAfterStart() {
        return EventValidation.endAfterStart(start, end, allDay);
    }

    @AssertTrue(message = "timeZone must be an IANA time zone such as America/Boise")
    public boolean isTimeZoneValid() {
        return EventValidation.validTimeZone(timeZone);
    }

    @AssertTrue(message = "occurrenceDate is required unless scope is ALL")
    public boolean isOccurrenceDateValid() {
        return scope == null || scope == DeleteScope.ALL || occurrenceDate != null;
    }

    public EventDTO toEventDTO() {
        return new EventDTO(name, note, start, end, allDay, timeZone, categoryId, recurrence);
    }
}
