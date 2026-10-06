package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.AccessLevel;

/** A guest with access to a calendar, as listed for the owner. */
public record CalendarPermissionResponseDTO(Long userId, String displayName, String email, AccessLevel accessLevel) {
}
