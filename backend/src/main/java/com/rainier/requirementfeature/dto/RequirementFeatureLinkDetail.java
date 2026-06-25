/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.dto;

import com.rainier.requirementfeature.domain.RequirementFeatureLink;
import java.time.Instant;

/** Response DTO for {@link RequirementFeatureLink}. */
public class RequirementFeatureLinkDetail {

  private Long id;
  private Long requirementId;
  private Long featureId;
  private Instant linkedAt;
  private Long linkedByUserId;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static RequirementFeatureLinkDetail from(RequirementFeatureLink link) {
    RequirementFeatureLinkDetail dto = new RequirementFeatureLinkDetail();
    dto.id = link.getId();
    dto.requirementId = link.getRequirementId();
    dto.featureId = link.getFeatureId();
    dto.linkedAt = link.getLinkedAt();
    dto.linkedByUserId = link.getLinkedByUserId();
    dto.createTime = link.getCreateTime();
    dto.updateTime = link.getUpdateTime();
    dto.createBy = link.getCreateBy();
    dto.updateBy = link.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public Long getFeatureId() {
    return featureId;
  }

  public Instant getLinkedAt() {
    return linkedAt;
  }

  public Long getLinkedByUserId() {
    return linkedByUserId;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public String getCreateBy() {
    return createBy;
  }

  public String getUpdateBy() {
    return updateBy;
  }
}
