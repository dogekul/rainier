/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/operations} (v0.0.44). Created at stage=MAINTENANCE / status=ACTIVE. */
public class OperationCreateRequest {

  @NotBlank
  @Size(max = 100)
  private String customerName;

  @NotBlank
  @Size(max = 200)
  private String title;

  private Long opsOwnerUserId;
  private Long projectId;
  /** v0.0.58 — 可选来源商机 id（手动建运营单可不传；ACCEPTANCE 自动建走 createForAcceptedOpportunity）。 */
  private Long opportunityId;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

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
