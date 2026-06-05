/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

/**
 * Thrown by service layers when an operation violates a business invariant (unique constraint,
 * FK-protected delete, tree-cycle, etc.).
 *
 * <p>Translated to HTTP 409 by {@code GlobalExceptionHandler}.
 */
public class ConflictException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConflictException(String message) {
    super(message);
  }
}
