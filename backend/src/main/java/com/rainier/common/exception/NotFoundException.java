/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

/**
 * Thrown by service layers when a requested entity does not exist.
 *
 * <p>Translated to HTTP 404 by {@code GlobalExceptionHandler}.
 */
public class NotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NotFoundException(String message) {
    super(message);
  }
}
