/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.dto;

import com.rainier.sprint.domain.Sprint;
import com.rainier.sprintfeature.domain.SprintFeatureLink;

/** A sprint enriched for the {@code GET /api/features/{id}/sprints} reverse query. */
public class FeatureSprintView {

  private Long linkId;
  private Long sprintId;
  private String code;
  private String name;
  private String status;
  private Long requirementId;
  private Long productId;

  public static FeatureSprintView from(Sprint s, SprintFeatureLink link) {
    FeatureSprintView v = new FeatureSprintView();
    v.linkId = link.getId();
    v.sprintId = s.getId();
    v.code = s.getCode();
    v.name = s.getName();
    v.status = s.getStatus();
    v.requirementId = s.getRequirementId();
    v.productId = s.getProductId();
    return v;
  }

  public Long getLinkId() {
    return linkId;
  }

  public Long getSprintId() {
    return sprintId;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getStatus() {
    return status;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public Long getProductId() {
    return productId;
  }
}
