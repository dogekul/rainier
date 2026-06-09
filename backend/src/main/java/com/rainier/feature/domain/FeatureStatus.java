/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 3-state machine for {@link Feature}: PLANNING / ACTIVE / DEPRECATED. */
public final class FeatureStatus {
  public static final String PLANNING = "PLANNING";
  public static final String ACTIVE = "ACTIVE";
  public static final String DEPRECATED = "DEPRECATED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PLANNING, ACTIVE, DEPRECATED)));

  private FeatureStatus() {}
}
