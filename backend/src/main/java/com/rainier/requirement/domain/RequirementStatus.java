/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Status values for {@link Requirement}. */
public final class RequirementStatus {
  public static final String DRAFT = "DRAFT";
  public static final String IN_REVIEW = "IN_REVIEW";
  public static final String APPROVED = "APPROVED";
  public static final String IN_DEV = "IN_DEV";
  public static final String DELIVERED = "DELIVERED";
  public static final String DEPRECATED = "DEPRECATED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(DRAFT, IN_REVIEW, APPROVED, IN_DEV, DELIVERED, DEPRECATED)));

  private RequirementStatus() {}
}
