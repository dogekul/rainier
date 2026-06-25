/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * AI 错误公示板的一条记录（v0.0.68, A4）。AI 出错的事实必须公开化 —— 飞轮"信任契约"的反面证据。本版
 * 仅手动 record；后续可由 monitor 自动检测填充。
 */
@Entity
@Table(name = "rainier_ai_error")
public class AiError extends BaseEntity {

  /** 错误发生时间。 */
  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  /** AI 做了什么（动作描述），例如 "SYNC_TASK_STATUS"。 */
  @Column(name = "ai_action", nullable = false, length = 128)
  private String aiAction;

  /** 错误的简要描述。 */
  @Column(name = "error_desc", nullable = false, length = 512)
  private String errorDesc;

  /** 涉及的实体类型，例如 "TASK"（可空）。 */
  @Column(name = "affected_entity_type", length = 64)
  private String affectedEntityType;

  /** 涉及的实体 id（可空）。 */
  @Column(name = "affected_entity_id")
  private Long affectedEntityId;

  /** 根因分类：DATA / MODEL / RULE（可空）。 */
  @Column(name = "root_cause", length = 16)
  private String rootCause;

  /** 状态：OPEN（默认） / FIXED。 */
  @Column(nullable = false, length = 16)
  private String status = AiErrorStatus.OPEN;

  /** 修复采取的措施（可空，markFixed 时填充）。 */
  @Column(name = "fix_action", length = 512)
  private String fixAction;

  /** 详细证据（可空）。 */
  @Lob
  @Column(name = "evidence")
  private String evidence;

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }

  public String getAiAction() {
    return aiAction;
  }

  public void setAiAction(String aiAction) {
    this.aiAction = aiAction;
  }

  public String getErrorDesc() {
    return errorDesc;
  }

  public void setErrorDesc(String errorDesc) {
    this.errorDesc = errorDesc;
  }

  public String getAffectedEntityType() {
    return affectedEntityType;
  }

  public void setAffectedEntityType(String affectedEntityType) {
    this.affectedEntityType = affectedEntityType;
  }

  public Long getAffectedEntityId() {
    return affectedEntityId;
  }

  public void setAffectedEntityId(Long affectedEntityId) {
    this.affectedEntityId = affectedEntityId;
  }

  public String getRootCause() {
    return rootCause;
  }

  public void setRootCause(String rootCause) {
    this.rootCause = rootCause;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFixAction() {
    return fixAction;
  }

  public void setFixAction(String fixAction) {
    this.fixAction = fixAction;
  }

  public String getEvidence() {
    return evidence;
  }

  public void setEvidence(String evidence) {
    this.evidence = evidence;
  }
}
