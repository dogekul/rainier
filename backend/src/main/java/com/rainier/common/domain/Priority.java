/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Priority levels shared across Demand and Requirement.
 *
 * <p>Stored as VARCHAR(16) strings — see design.md decision 2 (柔性优于 Java enum).
 */
public final class Priority {
  public static final String URGENT = "URGENT";
  public static final String HIGH = "HIGH";
  public static final String MEDIUM = "MEDIUM";
  public static final String LOW = "LOW";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(URGENT, HIGH, MEDIUM, LOW)));

  private Priority() {}
}
