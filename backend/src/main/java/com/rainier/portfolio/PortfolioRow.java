/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

/** One project's health rollup in a scoped portfolio (v0.0.28). */
public class PortfolioRow {

  private final Long projectId;
  private final String projectCode;
  private final String projectName;
  private final String projectStatus;
  private final Long organizationId;
  private final int openTasks;
  private final int overdueTasks;
  private final int blockedTasks;
  private final int overdueMilestones;
  private final String ryg;

  public PortfolioRow(
      Long projectId,
      String projectCode,
      String projectName,
      String projectStatus,
      Long organizationId,
      int openTasks,
      int overdueTasks,
      int blockedTasks,
      int overdueMilestones,
      String ryg) {
    this.projectId = projectId;
    this.projectCode = projectCode;
    this.projectName = projectName;
    this.projectStatus = projectStatus;
    this.organizationId = organizationId;
    this.openTasks = openTasks;
    this.overdueTasks = overdueTasks;
    this.blockedTasks = blockedTasks;
    this.overdueMilestones = overdueMilestones;
    this.ryg = ryg;
  }

  public Long getProjectId() {
    return projectId;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public String getProjectName() {
    return projectName;
  }

  public String getProjectStatus() {
    return projectStatus;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public int getOpenTasks() {
    return openTasks;
  }

  public int getOverdueTasks() {
    return overdueTasks;
  }

  public int getBlockedTasks() {
    return blockedTasks;
  }

  public int getOverdueMilestones() {
    return overdueMilestones;
  }

  public String getRyg() {
    return ryg;
  }
}
