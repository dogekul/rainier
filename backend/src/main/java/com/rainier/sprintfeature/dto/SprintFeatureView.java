/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.dto;

import com.rainier.feature.domain.Feature;
import com.rainier.sprintfeature.domain.SprintFeatureLink;

/** A feature enriched for the {@code GET /api/sprints/{id}/features} reverse query. */
public class SprintFeatureView {

  private Long linkId;
  private Long featureId;
  private String code;
  private String name;
  private String status;
  private Long moduleId;

  public static SprintFeatureView from(Feature f, SprintFeatureLink link) {
    SprintFeatureView v = new SprintFeatureView();
    v.linkId = link.getId();
    v.featureId = f.getId();
    v.code = f.getCode();
    v.name = f.getName();
    v.status = f.getStatus();
    v.moduleId = f.getModuleId();
    return v;
  }

  public Long getLinkId() {
    return linkId;
  }

  public Long getFeatureId() {
    return featureId;
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

  public Long getModuleId() {
    return moduleId;
  }
}
