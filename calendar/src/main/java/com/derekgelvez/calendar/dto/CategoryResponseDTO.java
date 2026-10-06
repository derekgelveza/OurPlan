package com.derekgelvez.calendar.dto;

import com.derekgelvez.calendar.model.Category;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CategoryResponseDTO(Long id, String name, String colour, @JsonProperty("isDefault") boolean isDefault) {

    public static CategoryResponseDTO from(Category category) {
        return new CategoryResponseDTO(category.getId(), category.getName(), category.getColor(), category.isDefault());
    }
}
