/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** v0.0.58 — 运营问题清单严重度（HIGH/MEDIUM/LOW）。 */
public final class IssueSeverity {
  public static final String HIGH = "HIGH";
  public static final String MEDIUM = "MEDIUM";
  public static final String LOW = "LOW";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(HIGH, MEDIUM, LOW)));

  private IssueSeverity() {}
}
