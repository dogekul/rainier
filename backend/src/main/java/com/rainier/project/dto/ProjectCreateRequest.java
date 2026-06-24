/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/projects}. */
public class ProjectCreateRequest {

  /** v0.0.49 — 服务端自动生成（{类型前缀}-{自增id}），此处输入一律忽略；保留字段仅为向后兼容。 */
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 2000)
  private String description;

  @Size(max = 16)
  private String status;

  /**
   * Owner is a business field — must be selected explicitly (frontend defaults to current user).
   */
  @NotNull private Long ownerUserId;

  /** v0.0.28 — optional org node (department/domain/team) for portfolio scoping. */
  private Long organizationId;

  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean enabled;

  /** v0.0.16 — optional; omitted → defaults to CASUAL in the service. v0.0.48 widens to 32 (EXTERNAL_DELIVERY=17). */
  @Size(max = 32)
  private String projectType;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getProjectType() {
    return projectType;
  }

  public void setProjectType(String projectType) {
    this.projectType = projectType;
  }
}
