package com.derekgelvez.user.responses;

import java.util.Map;

/** Error body for every failed request; same JSON shape as the calendar's ErrorResponseDTO. */
public record ErrorResponse(String code, String message, Map<String, Object> details) {
}
