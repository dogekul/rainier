/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

/** Thrown when a request payload is missing required fields or fails validation. */
public class BadRequestException extends RuntimeException {

  public BadRequestException(String message) {
    super(message);
  }
}
