/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.dto;

import com.rainier.demand.domain.Demand;
import com.rainier.demandrequirement.domain.DemandRequirementLink;

/**
 * Enriched view of a Demand as a source for a Requirement — bundles demand fields with the link's
 * id and type.
 */
public class SourceDemandView {

  private Long demandId;
  private String title;
  private String description;
  private Long submitterUserId;
  private String status;
  private String priority;
  private String source;
  private Long linkId;
  private String linkType;

  public static SourceDemandView from(Demand d, DemandRequirementLink link) {
    SourceDemandView v = new SourceDemandView();
    v.demandId = d.getId();
    v.title = d.getTitle();
    v.description = d.getDescription();
    v.submitterUserId = d.getSubmitterUserId();
    v.status = d.getStatus();
    v.priority = d.getPriority();
    v.source = d.getSource();
    v.linkId = link.getId();
    v.linkType = link.getLinkType();
    return v;
  }

  public Long getDemandId() {
    return demandId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Long getSubmitterUserId() {
    return submitterUserId;
  }

  public String getStatus() {
    return status;
  }

  public String getPriority() {
    return priority;
  }

  public String getSource() {
    return source;
  }

  public Long getLinkId() {
    return linkId;
  }

  public String getLinkType() {
    return linkType;
  }
}
