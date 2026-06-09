/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** 5-state machine for {@link Task} — simpler than Story (no DRAFT/READY split). */
public final class TaskStatus {
  public static final String TODO = "TODO";
  public static final String IN_PROGRESS = "IN_PROGRESS";
  public static final String DONE = "DONE";
  public static final String BLOCKED = "BLOCKED";
  public static final String CANCELLED = "CANCELLED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(TODO, IN_PROGRESS, DONE, BLOCKED, CANCELLED)));

  private TaskStatus() {}
}
