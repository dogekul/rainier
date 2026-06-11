/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.dto;

import javax.validation.constraints.NotNull;

/** Payload for {@code POST /api/sprint-features}. */
public class SprintFeatureLinkCreateRequest {

  @NotNull private Long sprintId;

  @NotNull private Long featureId;

  public Long getSprintId() {
    return sprintId;
  }

  public void setSprintId(Long sprintId) {
    this.sprintId = sprintId;
  }

  public Long getFeatureId() {
    return featureId;
  }

  public void setFeatureId(Long featureId) {
    this.featureId = featureId;
  }
}
