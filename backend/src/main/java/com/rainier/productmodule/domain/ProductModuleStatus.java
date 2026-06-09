/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 3-state machine for {@link ProductModule}: PLANNING / ACTIVE / DEPRECATED. */
public final class ProductModuleStatus {
  public static final String PLANNING = "PLANNING";
  public static final String ACTIVE = "ACTIVE";
  public static final String DEPRECATED = "DEPRECATED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PLANNING, ACTIVE, DEPRECATED)));

  private ProductModuleStatus() {}
}
