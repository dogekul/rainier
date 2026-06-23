/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/opportunities/{id}/advance} (v0.0.44). {@code decision} (PASS/REJECT) is
 * required only when the current stage is a gate (商机/投标/合同) — validated in the service.
 */
public class OpportunityAdvanceRequest {

  @Size(max = 16)
  private String decision;

  @Size(max = 500)
  private String note;

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
