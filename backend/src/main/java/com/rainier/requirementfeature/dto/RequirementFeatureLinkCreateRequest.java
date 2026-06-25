/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.dto;

import javax.validation.constraints.NotNull;

/** Payload for {@code POST /api/requirement-features}. */
public class RequirementFeatureLinkCreateRequest {

  @NotNull private Long requirementId;
  @NotNull private Long featureId;

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public Long getFeatureId() {
    return featureId;
  }

  public void setFeatureId(Long featureId) {
    this.featureId = featureId;
  }
}
