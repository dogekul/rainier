/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

import com.rainier.story.domain.Story;
import java.time.Instant;

/**
 * One Story awaiting review by the current user (v0.0.39) — a row in the「我的评审」queue. Core fields
 * from the Story; projectName / sprintName / ownerName enriched by {@code MeReviewService}.
 */
public class PendingReview {

  private Long storyId;
  private String code;
  private String title;
  private String status;
  private String priority;
  private String reviewStatus;
  private Long projectId;
  private String projectName;
  private Long sprintId;
  private String sprintName;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private Instant createTime;

  public static PendingReview from(Story s) {
    PendingReview r = new PendingReview();
    r.storyId = s.getId();
    r.code = s.getCode();
    r.title = s.getTitle();
    r.status = s.getStatus();
    r.priority = s.getPriority();
    r.reviewStatus = s.getReviewStatus();
    r.projectId = s.getProjectId();
    r.sprintId = s.getSprintId();
    r.ownerUserId = s.getOwnerUserId();
    r.createTime = s.getCreateTime();
    return r;
  }

  public Long getStoryId() {
    return storyId;
  }

  public String getCode() {
    return code;
  }

  public String getTitle() {
    return title;
  }

  public String getStatus() {
    return status;
  }

  public String getPriority() {
    return priority;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public Long getProjectId() {
    return projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public Long getSprintId() {
    return sprintId;
  }

  public String getSprintName() {
    return sprintName;
  }

  public void setSprintName(String sprintName) {
    this.sprintName = sprintName;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getOwnerLoginName() {
    return ownerLoginName;
  }

  public void setOwnerLoginName(String ownerLoginName) {
    this.ownerLoginName = ownerLoginName;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
