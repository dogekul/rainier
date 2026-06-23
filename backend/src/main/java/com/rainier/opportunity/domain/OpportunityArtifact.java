/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A 流转产出物 (v0.0.45) — a structured text deliverable (《商机调研报告》/《决策评审纪要》) produced at a
 * gated transition. Append-only: created only via the advance gate, no update/delete API. {@code createBy}
 * (BaseEntity) is the author; {@code createTime} the timestamp.
 */
@Entity
@Table(name = "rainier_opportunity_artifact")
public class OpportunityArtifact extends BaseEntity {

  @Column(name = "opportunity_id", nullable = false)
  private Long opportunityId;

  @Column(name = "type", nullable = false, length = 32)
  private String type;

  /** The source stage whose outward transition produced this artifact (e.g. LEAD / OPPORTUNITY). */
  @Column(name = "stage_from", length = 32)
  private String stageFrom;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "content", columnDefinition = "TEXT")
  private String content;

  /** For LINK-kind types (讲解材料/甲方诉求清单): the material URL. Null for report-kind types. */
  @Column(name = "link", length = 1000)
  private String link;

  /** For 决策评审纪要: the gate decision (PASS/REJECT) recorded with the minutes; null otherwise. */
  @Column(name = "decision", length = 16)
  private String decision;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getStageFrom() {
    return stageFrom;
  }

  public void setStageFrom(String stageFrom) {
    this.stageFrom = stageFrom;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }
}
