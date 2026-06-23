/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import com.rainier.opportunity.domain.ArtifactType;
import com.rainier.opportunity.domain.OpportunityArtifact;
import java.time.Instant;

/** Read view of a 流转产出物 (v0.0.45). */
public class OpportunityArtifactDetail {

  private Long id;
  private Long opportunityId;
  private String type;
  private String typeLabel;
  private String stageFrom;
  private String title;
  private String content;
  private String link;
  private String decision;
  private String author;
  private Instant createTime;

  public static OpportunityArtifactDetail from(OpportunityArtifact a) {
    OpportunityArtifactDetail d = new OpportunityArtifactDetail();
    d.id = a.getId();
    d.opportunityId = a.getOpportunityId();
    d.type = a.getType();
    d.typeLabel = ArtifactType.label(a.getType());
    d.stageFrom = a.getStageFrom();
    d.title = a.getTitle();
    d.content = a.getContent();
    d.link = a.getLink();
    d.decision = a.getDecision();
    d.author = a.getCreateBy();
    d.createTime = a.getCreateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public String getType() {
    return type;
  }

  public String getTypeLabel() {
    return typeLabel;
  }

  public String getStageFrom() {
    return stageFrom;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public String getLink() {
    return link;
  }

  public String getDecision() {
    return decision;
  }

  public String getAuthor() {
    return author;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
