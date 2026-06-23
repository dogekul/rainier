/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.Valid;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/opportunities/{id}/advance} (v0.0.44). {@code decision} (PASS/REJECT) is
 * required only when the current stage is a gate (商机/投标/合同) — validated in the service.
 *
 * <p>v0.0.45: {@code artifact} carries the 流转产出物 (《商机调研报告》/《决策评审纪要》) required by
 * {@code TransitionArtifactRules} for the source stage — validated and persisted atomically in the service.
 */
public class OpportunityAdvanceRequest {

  @Size(max = 16)
  private String decision;

  @Size(max = 500)
  private String note;

  @Valid private ArtifactInput artifact;

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

  public ArtifactInput getArtifact() {
    return artifact;
  }

  public void setArtifact(ArtifactInput artifact) {
    this.artifact = artifact;
  }
}
