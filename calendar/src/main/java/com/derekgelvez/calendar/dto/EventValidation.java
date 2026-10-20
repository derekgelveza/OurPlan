package com.derekgelvez.calendar.dto;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Cross-field rules shared by EventDTO and UpdateEventDTO. */
final class EventValidation {

    private EventValidation() {
    }

    static boolean endAfterStart(LocalDateTime start, LocalDateTime end, boolean allDay) {
        if (start == null || end == null) {
            return true; // reported by @NotNull
        }
        // All-day events compare dates only, so a one-day event (same start and end date) is valid.
        return allDay ? !end.toLocalDate().isBefore(start.toLocalDate()) : end.isAfter(start);
    }

    static boolean validTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return true; // reported by @NotBlank
        }
        try {
            ZoneId.of(timeZone);
            // Only region IDs (e.g. America/Boise) or UTC: fixed offsets and abbreviations such as
            // EST don't follow daylight saving, which recurring events depend on.
            return timeZone.contains("/") || "UTC".equals(timeZone);
        } catch (DateTimeException e) {
            return false;
        }
    }
}
