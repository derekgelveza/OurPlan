package com.derekgelvez.user.responses;

/** A JSON success body with a human-readable message, e.g. {"message": "Account verified successfully"}. */
public record MessageResponse(String message) {
}
