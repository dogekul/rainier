/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import com.rainier.opportunity.domain.StageActivity;
import java.time.Instant;
import java.time.LocalDate;

/** Read view of a {@link StageActivity} (v0.0.90). */
public class StageActivityDetail {

  private Long id;
  private Long opportunityId;
  private String stageCode;
  private String activityTitle;
  private String description;
  private Long assigneeUserId;
  private LocalDate dueDate;
  private String status;
  private Instant completedAt;
  private String createBy;
  private Instant createTime;

  public static StageActivityDetail from(StageActivity a) {
    StageActivityDetail d = new StageActivityDetail();
    d.id = a.getId();
    d.opportunityId = a.getOpportunityId();
    d.stageCode = a.getStageCode();
    d.activityTitle = a.getActivityTitle();
    d.description = a.getDescription();
    d.assigneeUserId = a.getAssigneeUserId();
    d.dueDate = a.getDueDate();
    d.status = a.getStatus();
    d.completedAt = a.getCompletedAt();
    d.createBy = a.getCreateBy();
    d.createTime = a.getCreateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public String getStageCode() {
    return stageCode;
  }

  public String getActivityTitle() {
    return activityTitle;
  }

  public String getDescription() {
    return description;
  }

  public Long getAssigneeUserId() {
    return assigneeUserId;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public String getCreateBy() {
    return createBy;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
