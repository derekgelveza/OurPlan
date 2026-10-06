package com.derekgelvez.calendar.exception;

import com.derekgelvez.calendar.dto.ErrorResponseDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns exceptions thrown by any service into HTTP responses with the right status code.
 * Controllers and services never build error responses themselves. The app reads
 * {@code code} to decide what to do (e.g. COLOR_IN_USE: ask the user, then resend with
 * reassignColor=true).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ColorInUseException.class)
    public ResponseEntity<ErrorResponseDTO> handle(ColorInUseException e) {
        return error(HttpStatus.CONFLICT, "COLOR_IN_USE", e.getMessage(),
                Map.of("conflictingCategoryId", e.getConflictingCategoryId()));
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handle(CategoryAlreadyExistsException e) {
        return error(HttpStatus.CONFLICT, "CATEGORY_ALREADY_EXISTS", e.getMessage(), Map.of());
    }

    @ExceptionHandler(CannotDeleteDefaultCategoryException.class)
    public ResponseEntity<ErrorResponseDTO> handle(CannotDeleteDefaultCategoryException e) {
        return error(HttpStatus.BAD_REQUEST, "CANNOT_DELETE_DEFAULT_CATEGORY", e.getMessage(), Map.of());
    }

    @ExceptionHandler(CannotModifyDefaultCategoryException.class)
    public ResponseEntity<ErrorResponseDTO> handle(CannotModifyDefaultCategoryException e) {
        return error(HttpStatus.BAD_REQUEST, "CANNOT_MODIFY_DEFAULT_CATEGORY", e.getMessage(), Map.of());
    }

    @ExceptionHandler(CalendarAccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handle(CalendarAccessDeniedException e) {
        return error(HttpStatus.FORBIDDEN, "CALENDAR_ACCESS_DENIED", e.getMessage(), Map.of());
    }

    @ExceptionHandler(InviteNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handle(InviteNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, "INVITE_NOT_VALID", e.getMessage(), Map.of());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handle(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", e.getMessage(), Map.of());
    }

    @ExceptionHandler(CalendarAlreadyExistsException.class)
    public ResponseEntity<ErrorResponseDTO> handle(CalendarAlreadyExistsException e) {
        return error(HttpStatus.CONFLICT, "CALENDAR_ALREADY_EXISTS", e.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponseDTO> handle(InvalidRequestException e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handle(MethodArgumentNotValidException e) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        e.getBindingResult().getGlobalErrors()
                .forEach(globalError -> details.putIfAbsent(globalError.getObjectName(), globalError.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", details);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponseDTO> handleBadRequest(Exception e) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Malformed request", Map.of());
    }

    /** Database-level invariants (unique names/colours, one calendar per user) caught under concurrent requests. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handle(DataIntegrityViolationException e) {
        return error(HttpStatus.CONFLICT, "CONFLICT", "The request conflicts with existing data", Map.of());
    }

    private ResponseEntity<ErrorResponseDTO> error(HttpStatus status, String code, String message,
                                                   Map<String, Object> details) {
        return ResponseEntity.status(status).body(new ErrorResponseDTO(code, message, details));
    }
}
