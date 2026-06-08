/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Status values for {@link Sprint}. 4-state machine, hierarchical not time-boxed. */
public final class SprintStatus {
  public static final String PLANNING = "PLANNING";
  public static final String ACTIVE = "ACTIVE";
  public static final String COMPLETED = "COMPLETED";
  public static final String CANCELLED = "CANCELLED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PLANNING, ACTIVE, COMPLETED, CANCELLED)));

  private SprintStatus() {}
}
