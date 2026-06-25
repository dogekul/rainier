/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/tasks}. Family pattern: required code/title/projectId, optional
 * status/priority/sprintId/storyId/assigneeUserId/dueDate. status defaults to TODO, priority to
 * MEDIUM if absent.
 */
public class TaskCreateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 200)
  private String title;

  @Size(max = 4000)
  private String description;

  @Size(max = 16)
  private String status;

  @Size(max = 16)
  private String priority;

  @NotNull private Long projectId;

  private Long sprintId;
  private Long storyId;
  private Long assigneeUserId;
  private LocalDate dueDate;

  @Size(max = 500)
  private String closeReason;

  /** v0.0.82: optional reviewer (validated to exist when non-null). */
  private Long reviewerUserId;

  /** v0.0.82: optional review state (validated against ReviewStatus.ALL when non-null). */
  @Size(max = 16)
  private String reviewStatus;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getSprintId() {
    return sprintId;
  }

  public void setSprintId(Long sprintId) {
    this.sprintId = sprintId;
  }

  public Long getStoryId() {
    return storyId;
  }

  public void setStoryId(Long storyId) {
    this.storyId = storyId;
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

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }

  public Long getReviewerUserId() {
    return reviewerUserId;
  }

  public void setReviewerUserId(Long reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }
}
