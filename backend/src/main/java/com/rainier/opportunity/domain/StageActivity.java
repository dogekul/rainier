/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.Instant;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 商机阶段活动清单 (v0.0.90) — "人在某个 stage 做了什么/要做什么"。与 {@link OpportunityArtifact} (产出物)
 * 互补：产出物是结果文档，活动是过程动作。每条活动归属一个 opportunity + stageCode；状态
 * PENDING/DONE/SKIPPED；DONE 写 completedAt。无门禁意义 — 纯任务清单视图。
 */
@Entity
@Table(name = "rainier_stage_activity")
public class StageActivity extends BaseEntity {

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_DONE = "DONE";
  public static final String STATUS_SKIPPED = "SKIPPED";

  @Column(name = "opportunity_id", nullable = false)
  private Long opportunityId;

  @Column(name = "stage_code", nullable = false, length = 32)
  private String stageCode;

  @Column(name = "activity_title", nullable = false, length = 200)
  private String activityTitle;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "assignee_user_id")
  private Long assigneeUserId;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "status", nullable = false, length = 16)
  private String status = STATUS_PENDING;

  @Column(name = "completed_at")
  private Instant completedAt;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public String getStageCode() {
    return stageCode;
  }

  public void setStageCode(String stageCode) {
    this.stageCode = stageCode;
  }

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }
}
