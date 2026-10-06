package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.AccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The invitee's email or phone number, and the access level they will get. */
public record ShareCalendarRequestDTO(
        @NotBlank
        @Pattern(regexp = "^([^@\\s]+@[^@\\s]+\\.[^@\\s]+|\\+?[0-9 ()\\-.]{7,20})$",
                message = "must be an email address or phone number")
        String inviteeContact,
        @NotNull AccessLevel accessLevel
) {
}
