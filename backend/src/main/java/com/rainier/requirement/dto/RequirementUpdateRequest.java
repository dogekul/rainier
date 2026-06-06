/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/requirements/{id}}.
 *
 * <p>v0.0.8: {@code ownerUserId} IS now mutable — semantic reversal from v0.0.6 (was "owner
 * immutable"). Service validates new owner exists. sourceDemandIds is still only on the create path
 * (manage links via /api/demand-requirements endpoints).
 */
public class RequirementUpdateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String title;

  @Size(max = 4000)
  private String description;

  @Size(max = 16)
  private String status;

  @Size(max = 16)
  private String priority;

  @Size(max = 8)
  private String complexity;

  private Long projectId;

  /** v0.0.8: owner IS mutable (admin can transfer requirement to another PO). */
  @NotNull private Long ownerUserId;

  @Size(max = 500)
  private String closeReason;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getComplexity() {
    return complexity;
  }

  public void setComplexity(String complexity) {
    this.complexity = complexity;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }
}
