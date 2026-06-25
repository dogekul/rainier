/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Write DTO for upsert (v0.0.89). */
public class ProjectImplementationUpsertRequest {

  @NotBlank
  private String scopeMarkdown;

  private Integer estimatedManDays;

  @Size(max = 2000)
  private String riskNotes;

  private String keyMilestonesJson;

  public String getScopeMarkdown() {
    return scopeMarkdown;
  }

  public void setScopeMarkdown(String scopeMarkdown) {
    this.scopeMarkdown = scopeMarkdown;
  }

  public Integer getEstimatedManDays() {
    return estimatedManDays;
  }

  public void setEstimatedManDays(Integer estimatedManDays) {
    this.estimatedManDays = estimatedManDays;
  }

  public String getRiskNotes() {
    return riskNotes;
  }

  public void setRiskNotes(String riskNotes) {
    this.riskNotes = riskNotes;
  }

  public String getKeyMilestonesJson() {
    return keyMilestonesJson;
  }

  public void setKeyMilestonesJson(String keyMilestonesJson) {
    this.keyMilestonesJson = keyMilestonesJson;
  }
}
