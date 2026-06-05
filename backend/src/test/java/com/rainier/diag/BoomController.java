/* (C) 2026 Rainier — internal use only. */
package com.rainier.diag;

import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnostic-only endpoints that throw representative exceptions — used to drive {@code
 * GlobalExceptionHandler} integration tests.
 *
 * <p>Lives under {@code src/test/java}; registered by tests via {@code @Import}. Never reaches a
 * production binary.
 */
@RestController
@Profile("test")
public class BoomController {

  @GetMapping("/api/_diag/boom")
  public String boom() {
    throw new RuntimeException("boom");
  }

  @GetMapping("/api/_diag/not-found")
  public String notFound() {
    throw new NotFoundException("widget 42 does not exist");
  }

  @GetMapping("/api/_diag/conflict")
  public String conflict() {
    throw new ConflictException("widget 42 already exists");
  }
}
