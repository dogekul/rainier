/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.dto;

import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectType;
import java.time.Instant;
import java.time.LocalDate;

/** Response DTO for {@link Project} read endpoints — enriched with owner user fields. */
public class ProjectDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String status;
  private Long ownerUserId;
  /** v0.0.8 enrichment — service join with User. */
  private String ownerName;

  private String ownerLoginName;
  /** v0.0.28 — optional org node for portfolio scoping. */
  private Long organizationId;
  /** v0.0.64 — enrichment: organization name + type for UI display. */
  private String organizationName;

  private String organizationType;
  /** v0.0.64 — 项目 PMO（单值，可空）. */
  private Long pmoUserId;

  private String pmoName;
  private String pmoLoginName;

  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean enabled;
  /** v0.0.16 — read-coalesced: a legacy NULL column surfaces as CASUAL (defends pre-backfill reads). */
  private String projectType;

  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static ProjectDetail from(Project p) {
    ProjectDetail dto = new ProjectDetail();
    dto.id = p.getId();
    dto.code = p.getCode();
    dto.name = p.getName();
    dto.description = p.getDescription();
    dto.status = p.getStatus();
    dto.ownerUserId = p.getOwnerUserId();
    dto.organizationId = p.getOrganizationId();
    dto.pmoUserId = p.getPmoUserId();
    dto.startDate = p.getStartDate();
    dto.endDate = p.getEndDate();
    dto.enabled = p.getEnabled();
    dto.projectType = p.getProjectType() == null ? ProjectType.CASUAL : p.getProjectType();
    dto.createTime = p.getCreateTime();
    dto.updateTime = p.getUpdateTime();
    dto.createBy = p.getCreateBy();
    dto.updateBy = p.getUpdateBy();
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

  public String getStatus() {
    return status;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public Long getOrganizationId() {
    return organizationId;
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

  public Boolean getEnabled() {
    return enabled;
  }

  public String getProjectType() {
    return projectType;
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

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public String getOrganizationType() {
    return organizationType;
  }

  public void setOrganizationType(String organizationType) {
    this.organizationType = organizationType;
  }

  public Long getPmoUserId() {
    return pmoUserId;
  }

  public void setPmoUserId(Long pmoUserId) {
    this.pmoUserId = pmoUserId;
  }

  public String getPmoName() {
    return pmoName;
  }

  public void setPmoName(String pmoName) {
    this.pmoName = pmoName;
  }

  public String getPmoLoginName() {
    return pmoLoginName;
  }

  public void setPmoLoginName(String pmoLoginName) {
    this.pmoLoginName = pmoLoginName;
  }
}
