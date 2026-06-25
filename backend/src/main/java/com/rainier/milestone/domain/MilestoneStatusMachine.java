/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.domain;

import com.rainier.common.exception.BadRequestException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * State machine for {@link Milestone#getStatus()} — v0.0.87 (C7).
 *
 * <p>Allowed transitions (anything else → {@link BadRequestException}):
 *
 * <pre>
 *   PLANNED     → IN_PROGRESS, CANCELLED
 *   IN_PROGRESS → DONE, CANCELLED
 *   DONE        → IN_PROGRESS   (撤销 done)
 *   CANCELLED   → PLANNED       (撤销取消)
 *   X           → X             (同状态 no-op，方便编辑其他字段时不触发校验)
 * </pre>
 *
 * <p>Note: {@code PLANNED → DONE} 必须先经过 {@code IN_PROGRESS}（防止跳过"进行中"）。
 */
public final class MilestoneStatusMachine {

  private static final Map<String, Set<String>> ALLOWED;

  static {
    Map<String, Set<String>> m = new HashMap<>();
    m.put(MilestoneStatus.PLANNED, asSet(MilestoneStatus.IN_PROGRESS, MilestoneStatus.CANCELLED));
    m.put(MilestoneStatus.IN_PROGRESS, asSet(MilestoneStatus.DONE, MilestoneStatus.CANCELLED));
    m.put(MilestoneStatus.DONE, asSet(MilestoneStatus.IN_PROGRESS));
    m.put(MilestoneStatus.CANCELLED, asSet(MilestoneStatus.PLANNED));
    ALLOWED = Collections.unmodifiableMap(m);
  }

  private static Set<String> asSet(String... values) {
    Set<String> s = new HashSet<>();
    for (String v : values) {
      s.add(v);
    }
    return Collections.unmodifiableSet(s);
  }

  /**
   * Validate a transition. Both sides must already be canonical (caller is responsible for {@link
   * MilestoneStatus#normalize(String)} first).
   *
   * @throws BadRequestException if the transition is not allowed
   */
  public static void validateTransition(String from, String to) {
    if (from == null || to == null) {
      throw new BadRequestException("illegal transition: " + from + " -> " + to);
    }
    if (from.equals(to)) {
      return; // no-op
    }
    Set<String> targets = ALLOWED.get(from);
    if (targets == null || !targets.contains(to)) {
      throw new BadRequestException("illegal transition: " + from + " -> " + to);
    }
  }

  private MilestoneStatusMachine() {}
}
