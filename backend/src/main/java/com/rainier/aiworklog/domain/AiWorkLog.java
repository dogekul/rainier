/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * An AI agent's proposed action (v0.0.43, the flywheel base). Every AI action is a {@code PROPOSED}
 * entry that MUST cite {@code evidence}; a human then accepts or rejects it (see {@link
 * AiWorkLogStatus}). Rejections are the KPI signal for AI quality. No {@code @SQLDelete} — the log is
 * kept; only {@code status} mutates on a decision.
 */
@Entity
@Table(name = "rainier_ai_work_log")
public class AiWorkLog extends BaseEntity {

  /** Which AI capability proposed it, e.g. STATUS_SYNC / RISK_RADAR / WEEKLY_REPORT. */
  @Column(name = "agent_type", nullable = false, length = 32)
  private String agentType;

  /** The proposed action, e.g. UPDATE_TASK_STATUS / FLAG_RISK. */
  @Column(nullable = false, length = 64)
  private String action;

  @Column(name = "target_type", length = 64)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(nullable = false, length = 512)
  private String summary;

  /** Evidence refs backing the proposal — NOT NULL: an AI proposal must not be a black box. */
  @Column(nullable = false, length = 2000)
  private String evidence;

  @Column(nullable = false, length = 16)
  private String status = AiWorkLogStatus.PROPOSED;

  @Column(name = "decided_by", length = 32)
  private String decidedBy;

  @Column(name = "decided_at")
  private Instant decidedAt;

  @Column(name = "reject_reason", length = 512)
  private String rejectReason;

  public String getAgentType() {
    return agentType;
  }

  public void setAgentType(String agentType) {
    this.agentType = agentType;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }

  public String getEvidence() {
    return evidence;
  }

  public void setEvidence(String evidence) {
    this.evidence = evidence;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public void setDecidedBy(String decidedBy) {
    this.decidedBy = decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public void setDecidedAt(Instant decidedAt) {
    this.decidedAt = decidedAt;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }
}
