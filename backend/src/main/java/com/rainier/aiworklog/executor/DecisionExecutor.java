/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.executor;

import com.rainier.aiworklog.domain.AiWorkLog;

/**
 * F1 (v0.0.100): plug-in that actually mutates a real entity when an {@link AiWorkLog} is ACCEPTED.
 * Implementations match by {@link AiWorkLog#getAction()} via {@link #supports(AiWorkLog)} and
 * return a JSON snapshot so the change can later be undone via {@link #reverse}.
 */
public interface DecisionExecutor {

  /** True when this executor knows how to run the given log's action. */
  boolean supports(AiWorkLog log);

  /**
   * Apply the action to the target entity. Return {@link ExecutorResult#ok(String)} with a JSON
   * snapshot of the pre-mutation state (consumed by {@link #reverse}) or
   * {@link ExecutorResult#skipped()} if nothing was changed.
   */
  ExecutorResult execute(AiWorkLog log);

  /** Undo a previously executed action using the stored snapshot. */
  ExecutorResult reverse(AiWorkLog log, String snapshotJson);
}
