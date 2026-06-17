/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/projects/{id}}. {@code code} is immutable; ownerUserId IS mutable.
 */
public class ProjectUpdateRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 2000)
  private String description;

  @NotBlank
  @Size(max = 16)
  private String status;

  /** v0.0.8: owner IS mutable (admin can transfer ownership). */
  @NotNull private Long ownerUserId;

  /** v0.0.28 — optional on update; null clears the org edge. */
  private Long organizationId;

  private LocalDate startDate;
  private LocalDate endDate;
  private Boolean enabled;

  /** v0.0.16 — optional on update; absent/null → preserve current value (no silent downgrade). */
  @Size(max = 16)
  private String projectType;

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
