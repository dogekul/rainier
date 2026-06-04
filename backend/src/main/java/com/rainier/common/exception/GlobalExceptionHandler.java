/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Translates exceptions to JSON {@code {"message": "..."}} responses.
 *
 * <p>Covers spec backend-scaffold → "未知 endpoint 返回结构化 JSON 404" and "未捕获异常返回结构化 JSON 500". Stack
 * traces are intentionally not included in the response body.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Map<String, String>> handleBadRequest(BadRequestException ex) {
    return body(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Map<String, String>> handleUnauthorized(UnauthorizedException ex) {
    return body(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<Map<String, String>> handleNotFound(NoHandlerFoundException ex) {
    return body(HttpStatus.NOT_FOUND, "Not Found: " + ex.getRequestURL());
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Map<String, String>> handleAny(Throwable ex) {
    LOG.error("Unhandled exception", ex);
    return body(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private static ResponseEntity<Map<String, String>> body(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(Collections.singletonMap("message", message));
  }
}
