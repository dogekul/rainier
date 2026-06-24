/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** v0.0.58 — 创建运营问题（POST /api/operations/{id}/issues）。 */
public class OperationIssueCreateRequest {

  @NotBlank
  @Size(max = 200)
  private String title;

  @Size(max = 4000)
  private String description;

  @Size(max = 8)
  private String severity;

  @NotNull private Long reporterUserId;

  private Long assigneeUserId;

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

  public Long getReporterUserId() {
    return reporterUserId;
  }

  public void setReporterUserId(Long reporterUserId) {
    this.reporterUserId = reporterUserId;
  }

  public Long getAssigneeUserId() {
    return assigneeUserId;
  }

  public void setAssigneeUserId(Long assigneeUserId) {
    this.assigneeUserId = assigneeUserId;
  }
}
