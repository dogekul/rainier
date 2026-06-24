/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.58 — 运营问题清单条目（Operation Issue）。挂在某条 Operation 下，标准字段：标题/描述/严重度/状态/上报人/负责人/关闭原因。
 * 软删除（沿用全局 @SQLDelete + @Where 模式）。
 */
@Entity
@Table(name = "rainier_operation_issue")
@SQLDelete(
    sql =
        "UPDATE rainier_operation_issue SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class OperationIssue extends BaseEntity {

  @Column(name = "operation_id", nullable = false)
  private Long operationId;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  /** HIGH / MEDIUM / LOW */
  @Column(nullable = false, length = 8)
  private String severity = IssueSeverity.MEDIUM;

  /** OPEN / IN_PROGRESS / RESOLVED / CLOSED */
  @Column(nullable = false, length = 16)
  private String status = IssueStatus.OPEN;

  @Column(name = "reporter_user_id", nullable = false)
  private Long reporterUserId;

  @Column(name = "assignee_user_id")
  private Long assigneeUserId;

  @Column(name = "close_reason", length = 500)
  private String closeReason;

  public Long getOperationId() {
    return operationId;
  }

  public void setOperationId(Long operationId) {
    this.operationId = operationId;
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

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }
}
