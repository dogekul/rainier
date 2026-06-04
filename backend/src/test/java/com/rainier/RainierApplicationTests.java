/* (C) 2026 Rainier — internal use only. */
package com.rainier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: Spring application context loads cleanly under the {@code test} profile.
 *
 * <p>Covers TC-BES-001 (contextLoads 用例通过) and forms the baseline for TC-TRT-001 (mvn test 全绿).
 */
@SpringBootTest
@ActiveProfiles("test")
class RainierApplicationTests {

  @Test
  void contextLoads() {
    // Intentionally empty: if context fails to load, the test fails.
  }
}
