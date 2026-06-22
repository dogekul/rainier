/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.dto;

import com.rainier.aiworklog.domain.AiWorkLog;
import java.time.Instant;

/** Read DTO for {@link AiWorkLog} (v0.0.43). */
public class AiWorkLogDetail {

  private Long id;
  private String agentType;
  private String action;
  private String targetType;
  private Long targetId;
  private String summary;
  private String evidence;
  private String status;
  private String decidedBy;
  private Instant decidedAt;
  private String rejectReason;
  private Instant createTime;

  public static AiWorkLogDetail from(AiWorkLog a) {
    AiWorkLogDetail dto = new AiWorkLogDetail();
    dto.id = a.getId();
    dto.agentType = a.getAgentType();
    dto.action = a.getAction();
    dto.targetType = a.getTargetType();
    dto.targetId = a.getTargetId();
    dto.summary = a.getSummary();
    dto.evidence = a.getEvidence();
    dto.status = a.getStatus();
    dto.decidedBy = a.getDecidedBy();
    dto.decidedAt = a.getDecidedAt();
    dto.rejectReason = a.getRejectReason();
    dto.createTime = a.getCreateTime();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public String getAgentType() {
    return agentType;
  }

  public String getAction() {
    return action;
  }

  public String getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public String getSummary() {
    return summary;
  }

  public String getEvidence() {
    return evidence;
  }

  public String getStatus() {
    return status;
  }

  public String getDecidedBy() {
    return decidedBy;
  }

  public Instant getDecidedAt() {
    return decidedAt;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
