package com.derekgelvez.calendar.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One entry per occurrence in the requested range, so a daily event shown over a week
 * appears 7 times. For all-day events end is the (inclusive) last day at midnight.
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
        CategoryResponseDTO category
) {
}
