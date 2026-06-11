/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.dto;

import com.rainier.sprintfeature.domain.SprintFeatureLink;
import java.time.Instant;

/** Response DTO for {@link SprintFeatureLink} write endpoints. */
public class SprintFeatureLinkDetail {

  private Long id;
  private Long sprintId;
  private Long featureId;
  private Instant createTime;
  private String createBy;

  public static SprintFeatureLinkDetail from(SprintFeatureLink link) {
    SprintFeatureLinkDetail dto = new SprintFeatureLinkDetail();
    dto.id = link.getId();
    dto.sprintId = link.getSprintId();
    dto.featureId = link.getFeatureId();
    dto.createTime = link.getCreateTime();
    dto.createBy = link.getCreateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public Long getSprintId() {
    return sprintId;
  }

  public Long getFeatureId() {
    return featureId;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public String getCreateBy() {
    return createBy;
  }
}
