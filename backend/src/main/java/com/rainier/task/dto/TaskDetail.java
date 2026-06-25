/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.dto;

import com.rainier.task.domain.Task;
import java.time.Instant;
import java.time.LocalDate;

/** GET response shape: 23 fields = 11 business + 8 enrichment + 4 audit. */
public class TaskDetail {

  private Long id;
  private String code;
  private String title;
  private String description;
  private String status;
  private String priority;

  private Long projectId;
  /** v0.0.11 enrichment — service join with Project. */
  private String projectName;

  private String projectCode;

  private Long sprintId;
  /** v0.0.11 enrichment — service join with Sprint (optional). */
  private String sprintCode;

  private String sprintName;

  private Long storyId;
  /** v0.0.11 enrichment — service join with Story (optional). */
  private String storyCode;

  private String storyTitle;

  private Long assigneeUserId;
  /** v0.0.11 enrichment — service join with User (optional). */
  private String assigneeName;

  private String assigneeLoginName;

  private LocalDate dueDate;
  private String closeReason;

  /** v0.0.82 — reviewer fields + enrichment. */
  private Long reviewerUserId;

  private String reviewerName;
  private String reviewStatus;

  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static TaskDetail from(Task t) {
    TaskDetail dto = new TaskDetail();
    dto.id = t.getId();
    dto.code = t.getCode();
    dto.title = t.getTitle();
    dto.description = t.getDescription();
    dto.status = t.getStatus();
    dto.priority = t.getPriority();
    dto.projectId = t.getProjectId();
    dto.sprintId = t.getSprintId();
    dto.storyId = t.getStoryId();
    dto.assigneeUserId = t.getAssigneeUserId();
    dto.dueDate = t.getDueDate();
    dto.closeReason = t.getCloseReason();
    dto.reviewerUserId = t.getReviewerUserId();
    dto.reviewStatus = t.getReviewStatus();
    dto.createTime = t.getCreateTime();
    dto.updateTime = t.getUpdateTime();
    dto.createBy = t.getCreateBy();
    dto.updateBy = t.getUpdateBy();
    return dto;
  }

  // getters + setters

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  public Long getSprintId() {
    return sprintId;
  }

  public void setSprintId(Long sprintId) {
    this.sprintId = sprintId;
  }

  public String getSprintCode() {
    return sprintCode;
  }

  public void setSprintCode(String sprintCode) {
    this.sprintCode = sprintCode;
  }

  public String getSprintName() {
    return sprintName;
  }

  public void setSprintName(String sprintName) {
    this.sprintName = sprintName;
  }

  public Long getStoryId() {
    return storyId;
  }

  public void setStoryId(Long storyId) {
    this.storyId = storyId;
  }

  public String getStoryCode() {
    return storyCode;
  }

  public void setStoryCode(String storyCode) {
    this.storyCode = storyCode;
  }

  public String getStoryTitle() {
    return storyTitle;
  }

  public void setStoryTitle(String storyTitle) {
    this.storyTitle = storyTitle;
  }

  public Long getAssigneeUserId() {
    return assigneeUserId;
  }

  public void setAssigneeUserId(Long assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public void setAssigneeName(String assigneeName) {
    this.assigneeName = assigneeName;
  }

  public String getAssigneeLoginName() {
    return assigneeLoginName;
  }

  public void setAssigneeLoginName(String assigneeLoginName) {
    this.assigneeLoginName = assigneeLoginName;
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

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant createTime) {
    this.createTime = createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public void setUpdateTime(Instant updateTime) {
    this.updateTime = updateTime;
  }

  public String getCreateBy() {
    return createBy;
  }

  public void setCreateBy(String createBy) {
    this.createBy = createBy;
  }

  public String getUpdateBy() {
    return updateBy;
  }

  public void setUpdateBy(String updateBy) {
    this.updateBy = updateBy;
  }

  public Long getReviewerUserId() {
    return reviewerUserId;
  }

  public void setReviewerUserId(Long reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
  }

  public String getReviewerName() {
    return reviewerName;
  }

  public void setReviewerName(String reviewerName) {
    this.reviewerName = reviewerName;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }
}
