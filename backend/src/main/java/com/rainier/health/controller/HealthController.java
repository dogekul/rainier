/* (C) 2026 Rainier — internal use only. */
package com.rainier.health.controller;

import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness probe endpoint for Docker / LB health checks.
 *
 * <p>Covers spec {@code health-check} → "服务正常运行时返回 UP".
 */
@RestController
public class HealthController {

  @GetMapping(path = "/api/health", produces = "application/json")
  public Map<String, String> health() {
    return Collections.singletonMap("status", "UP");
  }
}
