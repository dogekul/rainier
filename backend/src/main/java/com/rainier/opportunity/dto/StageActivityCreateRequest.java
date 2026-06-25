/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import java.time.LocalDate;

/** Create payload for {@link com.rainier.opportunity.domain.StageActivity} (v0.0.90). */
public class StageActivityCreateRequest {

  private String activityTitle;
  private String description;
  private Long assigneeUserId;
  private LocalDate dueDate;

  public String getActivityTitle() {
    return activityTitle;
  }

  public void setActivityTitle(String activityTitle) {
    this.activityTitle = activityTitle;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getAssigneeUserId() {
    return assigneeUserId;
  }

  public void setAssigneeUserId(Long assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }
}
