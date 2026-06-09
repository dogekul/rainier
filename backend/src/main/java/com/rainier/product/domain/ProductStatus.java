/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 4-state machine for {@link Product}: PLANNING / ACTIVE / SUNSET / ARCHIVED. */
public final class ProductStatus {
  public static final String PLANNING = "PLANNING";
  public static final String ACTIVE = "ACTIVE";
  public static final String SUNSET = "SUNSET";
  public static final String ARCHIVED = "ARCHIVED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PLANNING, ACTIVE, SUNSET, ARCHIVED)));

  private ProductStatus() {}
}
