/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** v0.0.58 — 更新运营问题（PUT /api/operation-issues/{id}）。包含状态切换与关闭原因。 */
public class OperationIssueUpdateRequest {

  @NotBlank
  @Size(max = 200)
  private String title;

  @Size(max = 4000)
  private String description;

  @Size(max = 8)
  private String severity;

  /** OPEN / IN_PROGRESS / RESOLVED / CLOSED */
  @Size(max = 16)
  private String status;

  private Long assigneeUserId;

  @Size(max = 500)
  private String closeReason;

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

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getAssigneeUserId() {
    return assigneeUserId;
  }

  public void setAssigneeUserId(Long assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }
}
