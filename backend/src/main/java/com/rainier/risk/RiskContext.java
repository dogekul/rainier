/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Read-only input bundle handed to each {@link RiskRule}. Carries the caller's identity, the
 * project ids in scope (resolved by {@code ScopeService} ahead of time), and the reference {@code
 * now} so rules are testable with a frozen clock.
 */
public class RiskContext {

  private final Long userId;
  private final List<Long> projectIds;
  private final LocalDateTime now;

  public RiskContext(Long userId, List<Long> projectIds, LocalDateTime now) {
    this.userId = userId;
    this.projectIds = projectIds == null ? Collections.<Long>emptyList() : projectIds;
    this.now = now;
  }

  public Long getUserId() {
    return userId;
  }

  public List<Long> getProjectIds() {
    return projectIds;
  }

  public LocalDateTime getNow() {
    return now;
  }
}
