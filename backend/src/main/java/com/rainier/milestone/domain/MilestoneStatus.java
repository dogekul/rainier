/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Status values for {@link Milestone} — extensible enum-as-constants, same pattern as {@code
 * ProjectStatus}.
 *
 * <p>v0.0.87 (C7 milestone-status-machine): canonical 4 状态 (PLANNED / IN_PROGRESS / DONE /
 * CANCELLED) + legacy 2 别名 (REACHED→DONE, MISSED→CANCELLED)。{@link #ALL} 仅含 canonical；输入校验通过
 * {@link #ACCEPTED} (canonical + legacy)；{@link #normalize(String)} 把 legacy 折叠到 canonical。
 */
public final class MilestoneStatus {
  public static final String PLANNED = "PLANNED";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String DONE = "DONE";
  public static final String CANCELLED = "CANCELLED";

  /** v0.0.17 legacy — accepted on input, normalized to {@link #DONE}. */
  public static final String REACHED = "REACHED";

  /** v0.0.17 legacy — accepted on input, normalized to {@link #CANCELLED}. */
  public static final String MISSED = "MISSED";

  /** Canonical statuses. */
  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PLANNED, IN_PROGRESS, DONE, CANCELLED)));

  /** Legacy aliases (REACHED / MISSED). */
  public static final Set<String> LEGACY_ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(REACHED, MISSED)));

  /** Canonical + legacy (used by input validation). */
  public static final Set<String> ACCEPTED;

  private static final Map<String, String> NORMALIZE;

  static {
    Set<String> accepted = new HashSet<>(ALL);
    accepted.addAll(LEGACY_ALL);
    ACCEPTED = Collections.unmodifiableSet(accepted);

    Map<String, String> n = new HashMap<>();
    n.put(REACHED, DONE);
    n.put(MISSED, CANCELLED);
    NORMALIZE = Collections.unmodifiableMap(n);
  }

  /** Map legacy → canonical; canonical pass-through; null pass-through; unknown returns input. */
  public static String normalize(String status) {
    if (status == null) {
      return null;
    }
    String mapped = NORMALIZE.get(status);
    return mapped != null ? mapped : status;
  }

  private MilestoneStatus() {}
}
