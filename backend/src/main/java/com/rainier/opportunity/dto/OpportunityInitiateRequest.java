/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/opportunities/{id}/initiate} (v0.0.44) — the 立项评审 gate handing a WON
 * opportunity into a delivery Project. {@code decision} (PASS/REJECT) validated in the service.
 */
public class OpportunityInitiateRequest {

  @NotNull private Long projectId;

  @Size(max = 16)
  private String decision;

  @Size(max = 500)
  private String note;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

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
