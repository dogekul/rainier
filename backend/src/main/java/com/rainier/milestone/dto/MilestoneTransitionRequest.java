/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** v0.0.87 (C7): explicit status transition request body for {@code POST /api/milestones/{id}/transition}. */
public class MilestoneTransitionRequest {

  @NotBlank private String to;

  @Size(max = 500)
  private String reason;

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
