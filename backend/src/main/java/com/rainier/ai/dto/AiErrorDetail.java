/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.dto;

import com.rainier.ai.domain.AiError;
import java.time.Instant;
import java.time.LocalDateTime;

/** Read DTO for {@link AiError} (v0.0.68, A4). */
public class AiErrorDetail {

  private Long id;
  private LocalDateTime occurredAt;
  private String aiAction;
  private String errorDesc;
  private String affectedEntityType;
  private Long affectedEntityId;
  private String rootCause;
  private String status;
  private String fixAction;
  private String evidence;
  private Instant createTime;

  public static AiErrorDetail from(AiError e) {
    AiErrorDetail d = new AiErrorDetail();
    d.id = e.getId();
    d.occurredAt = e.getOccurredAt();
    d.aiAction = e.getAiAction();
    d.errorDesc = e.getErrorDesc();
    d.affectedEntityType = e.getAffectedEntityType();
    d.affectedEntityId = e.getAffectedEntityId();
    d.rootCause = e.getRootCause();
    d.status = e.getStatus();
    d.fixAction = e.getFixAction();
    d.evidence = e.getEvidence();
    d.createTime = e.getCreateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public String getAiAction() {
    return aiAction;
  }

  public String getErrorDesc() {
    return errorDesc;
  }

  public String getAffectedEntityType() {
    return affectedEntityType;
  }

  public Long getAffectedEntityId() {
    return affectedEntityId;
  }

  public String getRootCause() {
    return rootCause;
  }

  public String getStatus() {
    return status;
  }

  public String getFixAction() {
    return fixAction;
  }

  public String getEvidence() {
    return evidence;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
