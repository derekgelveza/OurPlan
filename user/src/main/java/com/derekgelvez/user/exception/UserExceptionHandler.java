package com.derekgelvez.user.exception;

import com.derekgelvez.user.responses.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gives every user-app error the same JSON body as the calendar app:
 * {@code {"code": "...", "message": "...", "details": {...}}}.
 */
@RestControllerAdvice
public class UserExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(UserExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handle(ApiException e) {
        return error(e.getStatus(), e.getCode(), e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Malformed request", Map.of());
    }

    /** Unique email/username hit by two sign-ups at the same moment. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handle(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data", Map.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handle(AuthenticationException e) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handle(AccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to do this", Map.of());
    }

    /** Anything else: Spring's own errors keep their status (404, 405...); unknown ones become a generic 500. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e) {
        if (e instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatusCode status = springError.getStatusCode();
            HttpStatus resolved = HttpStatus.resolve(status.value());
            String code = resolved != null ? resolved.name() : "ERROR";
            return error(status, code, resolved != null ? resolved.getReasonPhrase() : "Request failed", Map.of());
        }
        log.error("Unhandled error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Something went wrong", Map.of());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatusCode status, String code, String message,
                                                Map<String, Object> details) {
        return ResponseEntity.status(status).body(new ErrorResponse(code, message, details));
    }
}
