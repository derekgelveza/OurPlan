package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.AccessLevel;
import jakarta.validation.constraints.NotNull;

/** The access level granted when the generated invite link is accepted. */
public record ShareCalendarRequestDTO(
        @NotNull AccessLevel accessLevel
) {
}
