/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Type values for {@link Project} — extensible enum-as-constants, same pattern as {@link
 * ProjectStatus}.
 *
 * <p>v0.0.16 seeded 轻量/正式 (CASUAL/FORMAL). v0.0.48 keeps 轻量 (CASUAL) and splits 正式 into three
 * formal sub-types — 主业-功能建设 ({@link #CORE_FEATURE}) / 主业-技术改造 ({@link #CORE_TECH}) /
 * 对外-交付 ({@link #EXTERNAL_DELIVERY}). The field is stored as VARCHAR so appending values needs no DB
 * enum migration; only enum-membership is validated. {@link #LEGACY_FORMAL} is retired (NOT in
 * {@link #ALL}) — startup backfill migrates existing FORMAL rows → CORE_FEATURE.
 */
public final class ProjectType {
  public static final String CASUAL = "CASUAL"; // 轻量
  public static final String CORE_FEATURE = "CORE_FEATURE"; // 主业-功能建设
  public static final String CORE_TECH = "CORE_TECH"; // 主业-技术改造
  public static final String EXTERNAL_DELIVERY = "EXTERNAL_DELIVERY"; // 对外-交付

  /** v0.0.16 「正式」value, retired in v0.0.48 — migrated to {@link #CORE_FEATURE} by backfill; not valid for create/update. */
  public static final String LEGACY_FORMAL = "FORMAL";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(CASUAL, CORE_FEATURE, CORE_TECH, EXTERNAL_DELIVERY)));

  private ProjectType() {}
}
