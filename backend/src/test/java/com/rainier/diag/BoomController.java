/* (C) 2026 Rainier — internal use only. */
package com.rainier.diag;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diagnostic-only endpoint that always throws — used to drive 500 path tests.
 *
 * <p>Lives under {@code src/test/java}; registered by {@link
 * com.rainier.common.exception.GlobalExceptionHandlerTest} via {@code @Import}. Never reaches a
 * production binary.
 */
@RestController
@Profile("test")
public class BoomController {

  @GetMapping("/api/_diag/boom")
  public String boom() {
    throw new RuntimeException("boom");
  }
}
