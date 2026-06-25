/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/tasks/{id}/review} (v0.0.82). {@code decision} must be APPROVED or
 * REJECTED. {@code reason} is required (≤500) when {@code decision == REJECTED} and is persisted
 * to {@code task.closeReason}; ignored on APPROVED.
 */
public class TaskReviewRequest {

  @NotBlank
  @Size(max = 16)
  private String decision;

  @Size(max = 500)
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
