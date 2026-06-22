/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/ai-work-logs/{id}/decision} (v0.0.43). {@code decision} = ACCEPTED |
 * REJECTED (validated in the service); {@code reason} is required when REJECTED (the KPI signal).
 */
public class AiWorkLogDecisionRequest {

  @NotBlank
  @Size(max = 16)
  private String decision;

  @Size(max = 512)
  private String reason;

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
