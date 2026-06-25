/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** v0.0.58 — 运营问题清单状态。v0.0.95 增加 CONVERTED (已转工单, 终态)。 */
public final class IssueStatus {
  public static final String OPEN = "OPEN";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String RESOLVED = "RESOLVED";
  public static final String CLOSED = "CLOSED";
  /** v0.0.95 — 已转为 Task, 终态; 由 convert-to-task 设置。 */
  public static final String CONVERTED = "CONVERTED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(OPEN, IN_PROGRESS, RESOLVED, CLOSED, CONVERTED)));

  private IssueStatus() {}
}
