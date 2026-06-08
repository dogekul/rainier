/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Status values for {@link Story}. BLOCKED is a first-class state (not a closeReason). */
public final class StoryStatus {
  public static final String DRAFT = "DRAFT";
  public static final String READY = "READY";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String DONE = "DONE";
  public static final String BLOCKED = "BLOCKED";
  public static final String CANCELLED = "CANCELLED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(DRAFT, READY, IN_PROGRESS, DONE, BLOCKED, CANCELLED)));

  private StoryStatus() {}
}
