/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

/**
 * v0.0.112 (H5) — counters for the 架构师 landing page (`GET /api/me/review-stats`).
 *
 * <ul>
 *   <li>{@code pendingStoryCount} / {@code pendingTaskCount} — Story / Task rows where the caller
 *       is the assigned reviewer and {@code reviewStatus = PENDING}.
 *   <li>{@code approvedThisWeek} / {@code rejectedThisWeek} — Story+Task rows where the caller is
 *       the reviewer, {@code reviewStatus} ∈ {APPROVED, REJECTED}, and {@code updateTime} is on
 *       or after the start of the current ISO week (Monday 00:00 UTC). Heuristic — there is no
 *       dedicated {@code reviewedAt} column yet (see proposal OutOfScope).
 * </ul>
 */
public class ReviewStats {
  private long pendingStoryCount;
  private long pendingTaskCount;
  private long approvedThisWeek;
  private long rejectedThisWeek;

  public ReviewStats() {}

  public ReviewStats(
      long pendingStoryCount,
      long pendingTaskCount,
      long approvedThisWeek,
      long rejectedThisWeek) {
    this.pendingStoryCount = pendingStoryCount;
    this.pendingTaskCount = pendingTaskCount;
    this.approvedThisWeek = approvedThisWeek;
    this.rejectedThisWeek = rejectedThisWeek;
  }

  public long getPendingStoryCount() {
    return pendingStoryCount;
  }

  public void setPendingStoryCount(long pendingStoryCount) {
    this.pendingStoryCount = pendingStoryCount;
  }

  public long getPendingTaskCount() {
    return pendingTaskCount;
  }

  public void setPendingTaskCount(long pendingTaskCount) {
    this.pendingTaskCount = pendingTaskCount;
  }

  public long getApprovedThisWeek() {
    return approvedThisWeek;
  }

  public void setApprovedThisWeek(long approvedThisWeek) {
    this.approvedThisWeek = approvedThisWeek;
  }

  public long getRejectedThisWeek() {
    return rejectedThisWeek;
  }

  public void setRejectedThisWeek(long rejectedThisWeek) {
    this.rejectedThisWeek = rejectedThisWeek;
  }
}
