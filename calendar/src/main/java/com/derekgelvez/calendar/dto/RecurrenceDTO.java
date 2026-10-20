package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.EventRecurrence;
import com.derekgelvez.calendar.model.RecurrenceFrequency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * Repeat rule for an event. interval defaults to 1; until defaults to three months after
 * the start date and is inclusive. daysOfWeek is required for WEEKLY and CUSTOM
 * (CUSTOM is a weekly recurrence with an explicit daysOfWeek selection).
 */
public record RecurrenceDTO(
        @NotNull RecurrenceFrequency frequency,
        @Min(1) Integer interval,
        Set<DayOfWeek> daysOfWeek,
        LocalDate until
) {

    /** The stored rule as returned to the app, or null for a one-time event. */
    public static RecurrenceDTO from(EventRecurrence rule) {
        if (rule == null) {
            return null;
        }
        return new RecurrenceDTO(rule.getFrequency(), rule.getInterval(), Set.copyOf(rule.getDaysOfWeek()), rule.getUntil());
    }

    @JsonIgnore
    @AssertTrue(message = "daysOfWeek is required for WEEKLY and CUSTOM")
    public boolean isDaysOfWeekValid() {
        boolean weekly = frequency == RecurrenceFrequency.WEEKLY || frequency == RecurrenceFrequency.CUSTOM;
        return !weekly || (daysOfWeek != null && !daysOfWeek.isEmpty());
    }
}
