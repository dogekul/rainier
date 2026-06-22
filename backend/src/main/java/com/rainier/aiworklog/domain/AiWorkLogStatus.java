/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Lifecycle of an {@link AiWorkLog} (v0.0.43, flywheel base). An AI action starts as {@code PROPOSED}
 * (with evidence) and a human then {@code ACCEPTED}s or {@code REJECTED}s it. Stored as VARCHAR(16) —
 * same flexible-string rationale as StoryStatus / ReviewStatus.
 */
public final class AiWorkLogStatus {
  public static final String PROPOSED = "PROPOSED";
  public static final String ACCEPTED = "ACCEPTED";
  public static final String REJECTED = "REJECTED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PROPOSED, ACCEPTED, REJECTED)));

  /** Terminal human decisions accepted by {@code POST /api/ai-work-logs/{id}/decision}. */
  public static final Set<String> DECISIONS =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ACCEPTED, REJECTED)));

  private AiWorkLogStatus() {}
}
