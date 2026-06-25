/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Task is a Project-scoped execution unit. Unlike Story (PO-authored vertical slice of a Sprint),
 * Task can be created ad-hoc by any user or system/AI agent. v0.0.11 invariants:
 *
 * <ul>
 *   <li>{@code projectId} NOT NULL (Project is the stable container).
 *   <li>{@code sprintId} nullable (optional Sprint link).
 *   <li>{@code storyId} nullable (optional Story link).
 *   <li>{@code assigneeUserId} nullable (unassigned task is valid).
 *   <li>Cross-layer consistency: if {@code sprintId} set then sprint→requirement→projectId must
 *       equal {@code projectId}; if {@code storyId} set then story.projectId must equal {@code
 *       projectId}; if both set then story.sprintId must equal {@code sprintId}. Validated by
 *       TaskService.create.
 *   <li>{@code projectId / sprintId / storyId} immutable after create.
 *   <li>{@code code} service-level unique (no DB UNIQUE — family pattern).
 *   <li>5-state machine via {@link TaskStatus}.
 *   <li>Soft-deleted via {@link BaseEntity#delFlag}.
 * </ul>
 */
@Entity
@Table(name = "rainier_task")
@SQLDelete(
    sql = "UPDATE rainier_task SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Task extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false, length = 16)
  private String priority;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "sprint_id")
  private Long sprintId;

  @Column(name = "story_id")
  private Long storyId;

  @Column(name = "assignee_user_id")
  private Long assigneeUserId;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "close_reason", length = 500)
  private String closeReason;

  /** v0.0.82: optional reviewer (soft FK→user). null = no review requested. */
  @Column(name = "reviewer_user_id")
  private Long reviewerUserId;

  /**
   * v0.0.82: review state — reuses {@link com.rainier.story.domain.ReviewStatus}. null = no
   * review.
   */
  @Column(name = "review_status", length = 16)
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
