/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/ai-work-logs} (v0.0.43) — an AI proposal. {@code evidence} is required:
 * an AI proposal must cite evidence (the flywheel trust contract).
 */
public class AiWorkLogCreateRequest {

  @NotBlank
  @Size(max = 32)
  private String agentType;

  @NotBlank
  @Size(max = 64)
  private String action;

  @Size(max = 64)
  private String targetType;

  private Long targetId;

  @NotBlank
  @Size(max = 512)
  private String summary;

  @NotBlank
  @Size(max = 2000)
  private String evidence;

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
}
