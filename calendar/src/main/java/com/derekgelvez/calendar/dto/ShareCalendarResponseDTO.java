package com.derekgelvez.calendar.dto;

import java.time.Instant;

/** The invite link the user can copy or send, and when it expires. */
public record ShareCalendarResponseDTO(String inviteLink, Instant expiresAt) {
}
