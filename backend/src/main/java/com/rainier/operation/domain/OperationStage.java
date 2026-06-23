/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * After-sales pipeline stages for an {@link Operation} (v0.0.44) — 客户全流程「售后环节」. Linear (no
 * gates): 回款/维保 → 运营 → 复购.
 */
public final class OperationStage {
  public static final String MAINTENANCE = "MAINTENANCE"; // 回款/维保
  public static final String OPERATING = "OPERATING"; // 运营
  public static final String REPURCHASE = "REPURCHASE"; // 复购

  public static final List<String> STAGE_ORDER =
      Collections.unmodifiableList(Arrays.asList(MAINTENANCE, OPERATING, REPURCHASE));

  public static final Set<String> ALL = Collections.unmodifiableSet(new HashSet<>(STAGE_ORDER));

  private OperationStage() {}
}
