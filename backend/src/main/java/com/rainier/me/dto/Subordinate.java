/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

/**
 * v0.0.111 (H4) — a direct subordinate of the caller (active member of an org the caller HEADs).
 * Carries the minimum the「我的下属」list table needs + a contribution summary for quick triage.
 */
public class Subordinate {

  private final Long id;
  private final String loginName;
  private final String displayName;
  private final String primaryOrgName;
  private final ContributionSummary contributionSummary;

  public Subordinate(
      Long id,
      String loginName,
      String displayName,
      String primaryOrgName,
      ContributionSummary contributionSummary) {
    this.id = id;
    this.loginName = loginName;
    this.displayName = displayName;
    this.primaryOrgName = primaryOrgName;
    this.contributionSummary = contributionSummary;
  }

  public Long getId() {
    return id;
  }

  public String getLoginName() {
    return loginName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getPrimaryOrgName() {
    return primaryOrgName;
  }

  public ContributionSummary getContributionSummary() {
    return contributionSummary;
  }

  /** Slim version of ContributionMetricsService output — only what the list view shows. */
  public static class ContributionSummary {
    private final long weeklyTasksDone;
    private final long totalTasks;

    public ContributionSummary(long weeklyTasksDone, long totalTasks) {
      this.weeklyTasksDone = weeklyTasksDone;
      this.totalTasks = totalTasks;
    }

    public long getWeeklyTasksDone() {
      return weeklyTasksDone;
    }

    public long getTotalTasks() {
      return totalTasks;
    }
  }
}
