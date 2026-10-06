package com.derekgelvez.calendar.dto;

import java.util.Map;

/** Returned for every error by GlobalExceptionHandler, e.g. code COLOR_IN_USE with details.conflictingCategoryId. */
public record ErrorResponseDTO(String code, String message, Map<String, Object> details) {
}
