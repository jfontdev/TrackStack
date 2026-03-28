package com.jfontdev.trackstack.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler that maps application exceptions to HTTP responses.
 * <p>
 * By centralizing exception handling here, controllers remain clean and focused
 * on request routing. Each handler method maps a specific exception type to an
 * appropriate HTTP status code and error response body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles {@link NotFoundException} thrown when a requested entity does not exist.
     * <p>
     * Returns a 404 Not Found response with the exception message in the body.
     *
     * @param ex the not found exception
     * @return a map containing the error message
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    /**
     * Handles {@link MethodArgumentNotValidException} thrown when request body
     * validation fails (e.g., {@code @NotBlank} constraints).
     * <p>
     * Returns a 400 Bad Request response with a map of field names to error messages.
     *
     * @param ex the validation exception
     * @return a map containing field-level error messages
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field -> errors.put(field.getField(), field.getDefaultMessage()));

        return Map.of("errors", errors);
    }

    /**
     * Handles {@link DataIntegrityViolationException} thrown when a database
     * constraint is violated (e.g., unique constraint on tag name, foreign key
     * violations).
     * <p>
     * Returns a 409 Conflict response with a generic error message. We intentionally
     * do not expose the underlying database error details to the client for security
     * reasons.
     *
     * @param ex the data integrity violation exception
     * @return a map containing the error message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return Map.of("error", "Operation violates a data integrity constraint.");
    }
}
