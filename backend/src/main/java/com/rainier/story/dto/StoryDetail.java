/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.dto;

import com.rainier.story.domain.Story;
import java.time.Instant;

/**
 * Response DTO for {@link Story} read endpoints, with enrichment from User / Requirement / Project.
 */
public class StoryDetail {

  private Long id;
  private String code;
  private String title;
  private String description;
  private String acceptanceCriteria;
  private String status;
  private String priority;
  private String complexity;
  /** v0.0.10: Story belongs to Sprint, which belongs to Requirement. */
  private Long sprintId;

  private String sprintCode;
  private String sprintName;
  private String sprintStatus;
  /** Enriched via 2-stage join sprint → requirement at read time. */
  private Long requirementId;

  private String requirementCode;
  private String requirementTitle;
  private Long projectId;
  private String projectName;
  private String projectCode;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private String closeReason;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static StoryDetail from(Story s) {
    StoryDetail dto = new StoryDetail();
    dto.id = s.getId();
    dto.code = s.getCode();
    dto.title = s.getTitle();
    dto.description = s.getDescription();
    dto.acceptanceCriteria = s.getAcceptanceCriteria();
    dto.status = s.getStatus();
    dto.priority = s.getPriority();
    dto.complexity = s.getComplexity();
    dto.sprintId = s.getSprintId();
    // requirementId populated by service.enrich via 2-stage join.
    dto.projectId = s.getProjectId();
    dto.ownerUserId = s.getOwnerUserId();
    dto.closeReason = s.getCloseReason();
    dto.createTime = s.getCreateTime();
    dto.updateTime = s.getUpdateTime();
    dto.createBy = s.getCreateBy();
    dto.updateBy = s.getUpdateBy();
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

  public String getAcceptanceCriteria() {
    return acceptanceCriteria;
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

  public Long getSprintId() {
    return sprintId;
  }

  public String getSprintCode() {
    return sprintCode;
  }

  public void setSprintCode(String sprintCode) {
    this.sprintCode = sprintCode;
  }

  public String getSprintName() {
    return sprintName;
  }

  public void setSprintName(String sprintName) {
    this.sprintName = sprintName;
  }

  public String getSprintStatus() {
    return sprintStatus;
  }

  public void setSprintStatus(String sprintStatus) {
    this.sprintStatus = sprintStatus;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public String getRequirementCode() {
    return requirementCode;
  }

  public void setRequirementCode(String requirementCode) {
    this.requirementCode = requirementCode;
  }

  public String getRequirementTitle() {
    return requirementTitle;
  }

  public void setRequirementTitle(String requirementTitle) {
    this.requirementTitle = requirementTitle;
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

  public String getCloseReason() {
    return closeReason;
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
