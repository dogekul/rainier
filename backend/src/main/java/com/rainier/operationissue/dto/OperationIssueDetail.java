/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.dto;

import com.rainier.operationissue.domain.OperationIssue;
import java.time.Instant;

/** v0.0.58 — 运营问题读 DTO。 */
public class OperationIssueDetail {

  private Long id;
  private Long operationId;
  private String title;
  private String description;
  private String severity;
  private String status;
  private Long reporterUserId;
  private String reporterName;
  private Long assigneeUserId;
  private String assigneeName;
  private String closeReason;
  private Instant createTime;
  private Instant updateTime;

  public static OperationIssueDetail from(OperationIssue i) {
    OperationIssueDetail d = new OperationIssueDetail();
    d.id = i.getId();
    d.operationId = i.getOperationId();
    d.title = i.getTitle();
    d.description = i.getDescription();
    d.severity = i.getSeverity();
    d.status = i.getStatus();
    d.reporterUserId = i.getReporterUserId();
    d.assigneeUserId = i.getAssigneeUserId();
    d.closeReason = i.getCloseReason();
    d.createTime = i.getCreateTime();
    d.updateTime = i.getUpdateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public Long getOperationId() {
    return operationId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getSeverity() {
    return severity;
  }

  public String getStatus() {
    return status;
  }

  public Long getReporterUserId() {
    return reporterUserId;
  }

  public String getReporterName() {
    return reporterName;
  }

  public void setReporterName(String reporterName) {
    this.reporterName = reporterName;
  }

  public Long getAssigneeUserId() {
    return assigneeUserId;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public void setAssigneeName(String assigneeName) {
    this.assigneeName = assigneeName;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }
}
