package com.derekgelvez.calendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One entry per occurrence in the requested range, so a daily event shown over a week
 * appears 7 times. For all-day events end is the (inclusive) last day at midnight.
 * For a repeating event, recurrence is its repeat rule and seriesStart the start of its first
 * occurrence (both are null-safe: recurrence is null for one-time events), so the app can edit
 * the whole series without changing its rule or start date by accident.
 */
public record EventResponseDTO(
        Long eventId,
        LocalDate occurrenceDate,
        String name,
        String note,
        LocalDateTime start,
        LocalDateTime end,
        boolean allDay,
        String timeZone,
        @JsonProperty("isRepeating") boolean isRepeating,
        CategoryResponseDTO category,
        RecurrenceDTO recurrence,
        LocalDateTime seriesStart
) {
}
