/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Status values for {@link Demand}. */
public final class DemandStatus {
  public static final String PENDING = "PENDING";
  public static final String IN_REVIEW = "IN_REVIEW";
  public static final String CONVERTED = "CONVERTED";
  public static final String DONE = "DONE";
  public static final String CLOSED = "CLOSED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PENDING, IN_REVIEW, CONVERTED, DONE, CLOSED)));

  private DemandStatus() {}
}
