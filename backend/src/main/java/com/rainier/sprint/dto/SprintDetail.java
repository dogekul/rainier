/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.dto;

import com.rainier.sprint.domain.Sprint;
import java.time.Instant;
import java.time.LocalDate;

/** Response DTO for {@link Sprint} read endpoints, enriched from User / Requirement / Project. */
public class SprintDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String goal;
  private String status;
  private Long requirementId;
  private String requirementCode;
  private String requirementTitle;
  private Long projectId;
  private String projectName;
  private String projectCode;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private LocalDate startDate;
  private LocalDate endDate;
  /** Count of non-deleted Story rows attached to this Sprint. */
  private Long storyCount;

  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static SprintDetail from(Sprint s) {
    SprintDetail dto = new SprintDetail();
    dto.id = s.getId();
    dto.code = s.getCode();
    dto.name = s.getName();
    dto.description = s.getDescription();
    dto.goal = s.getGoal();
    dto.status = s.getStatus();
    dto.requirementId = s.getRequirementId();
    dto.ownerUserId = s.getOwnerUserId();
    dto.startDate = s.getStartDate();
    dto.endDate = s.getEndDate();
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

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getGoal() {
    return goal;
  }

  public String getStatus() {
    return status;
  }

  public Long getRequirementId() {
    return requirementId;
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

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
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

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public Long getStoryCount() {
    return storyCount;
  }

  public void setStoryCount(Long storyCount) {
    this.storyCount = storyCount;
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
