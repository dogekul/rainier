/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Status values for {@link Milestone} — extensible enum-as-constants, same pattern as {@code
 * ProjectStatus}. v0.0.17: three states, freely changeable (no state machine — only enum membership
 * is validated, matching the v0.0.16 "暂不校验" spirit).
 */
public final class MilestoneStatus {
  public static final String PLANNED = "PLANNED";
  public static final String REACHED = "REACHED";
  public static final String MISSED = "MISSED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PLANNED, REACHED, MISSED)));

  private MilestoneStatus() {}
}
