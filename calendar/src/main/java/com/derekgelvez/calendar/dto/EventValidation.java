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
        return allDay ? !end.toLocalDate().isBefore(start.toLocalDate()) : end.isAfter(start);
    }

    static boolean validTimeZone(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return true; // reported by @NotBlank
        }
        try {
            ZoneId.of(timeZone);
            return timeZone.contains("/") || "UTC".equals(timeZone);
        } catch (DateTimeException e) {
            return false;
        }
    }
}
