/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Status values for {@link Project}. */
public final class ProjectStatus {
  public static final String PLANNING = "PLANNING";
  public static final String ACTIVE = "ACTIVE";
  public static final String ON_HOLD = "ON_HOLD";
  public static final String DELIVERED = "DELIVERED";
  public static final String ARCHIVED = "ARCHIVED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PLANNING, ACTIVE, ON_HOLD, DELIVERED, ARCHIVED)));

  private ProjectStatus() {}
}
