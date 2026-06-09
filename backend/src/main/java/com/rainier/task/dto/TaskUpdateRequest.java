/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/tasks/{id}}. projectId / sprintId / storyId are NOT in this DTO —
 * parent relationships are creation-time immutable (Decision 11). assigneeUserId can be null to
 * unassign the task.
 */
public class TaskUpdateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 200)
  private String title;

  @Size(max = 4000)
  private String description;

  @NotBlank
  @Size(max = 16)
  private String status;

  @NotBlank
  @Size(max = 16)
  private String priority;

  private Long assigneeUserId;
  private LocalDate dueDate;

  @Size(max = 500)
  private String closeReason;

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
}
