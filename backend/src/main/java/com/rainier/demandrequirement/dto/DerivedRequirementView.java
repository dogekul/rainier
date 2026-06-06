/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.dto;

import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.requirement.domain.Requirement;

/**
 * Enriched view of a Requirement derived from a Demand — bundles requirement fields with the link's
 * id and type.
 */
public class DerivedRequirementView {

  private Long requirementId;
  private String code;
  private String title;
  private String description;
  private Long ownerUserId;
  private String status;
  private String priority;
  private String complexity;
  private Long linkId;
  private String linkType;

  public static DerivedRequirementView from(Requirement r, DemandRequirementLink link) {
    DerivedRequirementView v = new DerivedRequirementView();
    v.requirementId = r.getId();
    v.code = r.getCode();
    v.title = r.getTitle();
    v.description = r.getDescription();
    v.ownerUserId = r.getOwnerUserId();
    v.status = r.getStatus();
    v.priority = r.getPriority();
    v.complexity = r.getComplexity();
    v.linkId = link.getId();
    v.linkType = link.getLinkType();
    return v;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public String getCode() {
    return code;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public String getStatus() {
    return status;
  }

  public String getPriority() {
    return priority;
  }

  public String getComplexity() {
    return complexity;
  }

  public Long getLinkId() {
    return linkId;
  }

  public String getLinkType() {
    return linkType;
  }
}
