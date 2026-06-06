/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.dto;

import com.rainier.demandrequirement.domain.DemandRequirementLink;
import java.time.Instant;

/** Response DTO for {@link DemandRequirementLink} read endpoints. */
public class DemandRequirementLinkDetail {

  private Long id;
  private Long demandId;
  private Long requirementId;
  private String linkType;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static DemandRequirementLinkDetail from(DemandRequirementLink link) {
    DemandRequirementLinkDetail dto = new DemandRequirementLinkDetail();
    dto.id = link.getId();
    dto.demandId = link.getDemandId();
    dto.requirementId = link.getRequirementId();
    dto.linkType = link.getLinkType();
    dto.createTime = link.getCreateTime();
    dto.updateTime = link.getUpdateTime();
    dto.createBy = link.getCreateBy();
    dto.updateBy = link.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public Long getDemandId() {
    return demandId;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public String getLinkType() {
    return linkType;
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
