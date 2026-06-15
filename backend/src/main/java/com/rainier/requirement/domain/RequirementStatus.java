/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Status values for {@link Requirement}.
 *
 * <p>v0.0.19: state set adjusted to 草稿/审批中/分析中/实施中/已交付/已关闭. Freely changeable (no enforced
 * transitions — only enum membership is validated). Legacy values (IN_REVIEW/APPROVED/IN_DEV/
 * DEPRECATED) are remapped to the new set at startup by {@code RequirementStatusBackfill}.
 */
public final class RequirementStatus {
  public static final String DRAFT = "DRAFT";
  public static final String IN_APPROVAL = "IN_APPROVAL";
  public static final String IN_ANALYSIS = "IN_ANALYSIS";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String DELIVERED = "DELIVERED";
  public static final String CLOSED = "CLOSED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(DRAFT, IN_APPROVAL, IN_ANALYSIS, IN_PROGRESS, DELIVERED, CLOSED)));

  private RequirementStatus() {}
}
