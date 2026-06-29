/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.executor;

/**
 * Result of a {@link DecisionExecutor#execute} or {@link DecisionExecutor#reverse} call. {@code
 * executed} = whether the entity was mutated; {@code snapshot} = JSON describing the pre-mutation
 * state (used by reverse to restore) or {@code null} when nothing to revert.
 */
public final class ExecutorResult {

  private final boolean executed;
  private final String snapshot;

  private ExecutorResult(boolean executed, String snapshot) {
    this.executed = executed;
    this.snapshot = snapshot;
  }

  public static ExecutorResult ok(String snapshot) {
    return new ExecutorResult(true, snapshot);
  }

  public static ExecutorResult skipped() {
    return new ExecutorResult(false, null);
  }

  public boolean isExecuted() {
    return executed;
  }

  public String getSnapshot() {
    return snapshot;
  }
}
