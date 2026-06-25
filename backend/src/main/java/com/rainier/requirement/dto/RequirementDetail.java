/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.dto;

import com.rainier.requirement.domain.Requirement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/** Response DTO for {@link Requirement} read endpoints. */
public class RequirementDetail {

  private Long id;
  private String code;
  private String title;
  private String description;
  private Long ownerUserId;
  /** v0.0.8 enrichment — service join with User (frontend RequirementsPage 显示 owner 列). */
  private String ownerName;

  private String ownerLoginName;
  private String status;
  private String priority;
  private String complexity;
  private Long projectId;
  /** v0.0.8 enrichment — service join with Project. */
  private String projectName;

  private String projectCode;
  private String closeReason;
  /** v0.0.19 — 期望交付日期 (可空). */
  private LocalDate expectedDate;
  /**
   * v0.0.10 enrichment — service join with Sprint (count of non-deleted Sprints). Replaces v0.0.9
   * storyCount.
   */
  private Long sprintCount;
  /** v0.0.56 — 来源商机（可空）。 */
  private Long opportunityId;
  /** v0.0.86 (C6) — directly linked Feature ids (empty list if none). */
  private List<Long> featureIds = Collections.emptyList();

  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static RequirementDetail from(Requirement r) {
    RequirementDetail dto = new RequirementDetail();
    dto.id = r.getId();
    dto.code = r.getCode();
    dto.title = r.getTitle();
    dto.description = r.getDescription();
    dto.ownerUserId = r.getOwnerUserId();
    dto.status = r.getStatus();
    dto.priority = r.getPriority();
    dto.complexity = r.getComplexity();
    dto.projectId = r.getProjectId();
    dto.closeReason = r.getCloseReason();
    dto.expectedDate = r.getExpectedDate();
    dto.opportunityId = r.getOpportunityId();
    dto.createTime = r.getCreateTime();
    dto.updateTime = r.getUpdateTime();
    dto.createBy = r.getCreateBy();
    dto.updateBy = r.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
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

  public String getOwnerName() {
    return ownerName;
  }

  public void setOwnerName(String ownerName) {
    this.ownerName = ownerName;
  }

  public String getOwnerLoginName() {
    return ownerLoginName;
  }

  public void setOwnerLoginName(String ownerLoginName) {
    this.ownerLoginName = ownerLoginName;
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

  public Long getProjectId() {
    return projectId;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public LocalDate getExpectedDate() {
    return expectedDate;
  }

  public Long getOpportunityId() {
    return opportunityId;
  }

  public List<Long> getFeatureIds() {
    return featureIds;
  }

  public void setFeatureIds(List<Long> featureIds) {
    this.featureIds = featureIds == null ? Collections.<Long>emptyList() : featureIds;
  }

  public Long getSprintCount() {
    return sprintCount;
  }

  public void setSprintCount(Long sprintCount) {
    this.sprintCount = sprintCount;
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
