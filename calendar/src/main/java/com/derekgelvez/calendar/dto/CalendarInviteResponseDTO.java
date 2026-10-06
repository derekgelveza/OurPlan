package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.AccessLevel;
import com.derekgelvez.calendar.model.InviteStatus;

import java.time.Instant;

/** A calendar's invite, as listed for the owner. */
public record CalendarInviteResponseDTO(
        Long inviteId,
        String inviteeContact,
        AccessLevel accessLevel,
        InviteStatus status,
        String inviteLink,
        Instant expiresAt
) {
}
