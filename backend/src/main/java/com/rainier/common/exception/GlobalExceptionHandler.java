/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Translates exceptions to structured JSON responses.
 *
 * <p>Response body always contains {@code message}; validation errors additionally include {@code
 * fieldErrors[]} with one entry per invalid field. Stack traces are intentionally never leaked.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {
    return body(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {
    return body(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFound(NotFoundException ex) {
    return body(HttpStatus.NOT_FOUND, ex.getMessage(), null);
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
    return body(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  /**
   * Handles both {@link MethodArgumentNotValidException} (from {@code @Valid @RequestBody}) and
   * {@link BindException} (from {@code @Valid} on query-string-bound DTOs). The former extends the
   * latter, so one handler covers both.
   */
  @ExceptionHandler(BindException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(BindException ex) {
    List<Map<String, String>> fieldErrors = new ArrayList<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      Map<String, String> entry = new LinkedHashMap<>();
      entry.put("field", fe.getField());
      entry.put("message", fe.getDefaultMessage());
      fieldErrors.add(entry);
    }
    return body(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<Map<String, Object>> handleNotFoundRoute(NoHandlerFoundException ex) {
    return body(HttpStatus.NOT_FOUND, "Not Found: " + ex.getRequestURL(), null);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Map<String, Object>> handleAny(Throwable ex) {
    LOG.error("Unhandled exception", ex);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", null);
  }

  private static ResponseEntity<Map<String, Object>> body(
      HttpStatus status, String message, List<Map<String, String>> fieldErrors) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("message", message);
    if (fieldErrors != null) {
      response.put("fieldErrors", fieldErrors);
    }
    return ResponseEntity.status(status).body(response);
  }
}
