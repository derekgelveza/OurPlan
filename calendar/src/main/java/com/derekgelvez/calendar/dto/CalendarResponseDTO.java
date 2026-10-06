package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.AccessLevel;
import com.fasterxml.jackson.annotation.JsonProperty;

/** accessLevel is null for the user's own calendar and set for calendars shared with them. */
public record CalendarResponseDTO(
        Long calendarId,
        String ownerName,
        @JsonProperty("isOwn") boolean isOwn,
        AccessLevel accessLevel
) {
}
