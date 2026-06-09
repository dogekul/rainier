/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 2-state machine for {@link ProductCategory}: organisational dimension only. */
public final class ProductCategoryStatus {
  public static final String ACTIVE = "ACTIVE";
  public static final String ARCHIVED = "ARCHIVED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ACTIVE, ARCHIVED)));

  private ProductCategoryStatus() {}
}
