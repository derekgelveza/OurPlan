package com.derekgelvez.user.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * An expected error with the HTTP status and machine-readable code the client should receive,
 * e.g. 401 INVALID_CREDENTIALS. Turned into an {@code ErrorResponse} by {@link UserExceptionHandler}.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
