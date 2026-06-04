/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

/** Thrown when an authenticated endpoint is accessed without a valid bearer token. */
public class UnauthorizedException extends RuntimeException {

  public UnauthorizedException(String message) {
    super(message);
  }
}
