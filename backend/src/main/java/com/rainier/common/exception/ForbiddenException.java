/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

/**
 * Thrown when an authenticated caller lacks the elevation (admin) required for an endpoint. Mapped to
 * HTTP 403 by {@code GlobalExceptionHandler}. Distinct from {@link UnauthorizedException} (401 — no /
 * invalid token).
 */
public class ForbiddenException extends RuntimeException {

  public ForbiddenException(String message) {
    super(message);
  }
}
