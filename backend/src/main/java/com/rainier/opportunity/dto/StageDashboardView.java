/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import java.util.List;

/**
 * 整合视图 (v0.0.90) — 单个 stage 的「活动清单」+「关联产出物」一次返回，避免前端两次往返。
 */
public class StageDashboardView {

  private Long opportunityId;
  private String stageCode;
  private List<StageActivityDetail> activities;
  private List<OpportunityArtifactDetail> artifacts;

  public StageDashboardView(
      Long opportunityId,
      String stageCode,
      List<StageActivityDetail> activities,
      List<OpportunityArtifactDetail> artifacts) {
    this.opportunityId = opportunityId;
    this.stageCode = stageCode;
    this.activities = activities;
    this.artifacts = artifacts;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public String getStageCode() {
    return stageCode;
  }

  public List<StageActivityDetail> getActivities() {
    return activities;
  }

  public List<OpportunityArtifactDetail> getArtifacts() {
    return artifacts;
  }
}
