/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/demand-requirements}. */
public class DemandRequirementLinkCreateRequest {

  @NotNull private Long demandId;

  @NotNull private Long requirementId;

  @NotBlank
  @Size(max = 16)
  private String linkType;

  public Long getDemandId() {
    return demandId;
  }

  public void setDemandId(Long demandId) {
    this.demandId = demandId;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public String getLinkType() {
    return linkType;
  }

  public void setLinkType(String linkType) {
    this.linkType = linkType;
  }
}
