/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/operations/{id}} (v0.0.44) — attributes only (stage via advance). */
public class OperationUpdateRequest {

  @NotBlank
  @Size(max = 100)
  private String customerName;

  @NotBlank
  @Size(max = 200)
  private String title;

  private Long opsOwnerUserId;
  private Long projectId;

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Long getOpsOwnerUserId() {
    return opsOwnerUserId;
  }

  public void setOpsOwnerUserId(Long opsOwnerUserId) {
    this.opsOwnerUserId = opsOwnerUserId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
}
