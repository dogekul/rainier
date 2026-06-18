/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Review states for {@link Story} (v0.0.39). {@code null} = no review requested. {@code PENDING} =
 * waiting on the assigned reviewer; {@code APPROVED} = 放行; {@code REJECTED} = 打回. Stored as
 * VARCHAR(16) — 柔性优于 enum (same rationale as {@link StoryStatus} / Priority), no migration on new
 * values.
 */
public final class ReviewStatus {
  public static final String PENDING = "PENDING";
  public static final String APPROVED = "APPROVED";
  public static final String REJECTED = "REJECTED";

  /** All assignable review states (what a Story's reviewStatus may be set to on create/update). */
  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PENDING, APPROVED, REJECTED)));

  /** Terminal decisions a reviewer may record via {@code POST /api/stories/{id}/review}. */
  public static final Set<String> DECISIONS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(APPROVED, REJECTED)));

  private ReviewStatus() {}
}
