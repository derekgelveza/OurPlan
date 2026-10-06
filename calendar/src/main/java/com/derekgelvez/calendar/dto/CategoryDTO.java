package com.derekgelvez.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/update request for a category. Colour is a hex code such as "#E53935". */
public record CategoryDTO(
        @NotBlank @Size(max = 50) String name,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "must be a hex colour like #E53935") String colour
) {
}
