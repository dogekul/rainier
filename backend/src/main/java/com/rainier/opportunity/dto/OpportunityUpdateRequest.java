/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/opportunities/{id}} (v0.0.44) — edits attributes only. {@code stage} /
 * {@code status} / {@code projectId} are NOT edited here (use advance / initiate).
 */
public class OpportunityUpdateRequest {

  @NotBlank
  @Size(max = 100)
  private String customerName;

  @NotBlank
  @Size(max = 200)
  private String title;

  private Long amount;
  private Long commercialOwnerUserId;
  private Long solutionOwnerUserId;
  private Long pmUserId;
  private Long opsOwnerUserId;

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

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }

  public Long getCommercialOwnerUserId() {
    return commercialOwnerUserId;
  }

  public void setCommercialOwnerUserId(Long commercialOwnerUserId) {
    this.commercialOwnerUserId = commercialOwnerUserId;
  }

  public Long getSolutionOwnerUserId() {
    return solutionOwnerUserId;
  }

  public void setSolutionOwnerUserId(Long solutionOwnerUserId) {
    this.solutionOwnerUserId = solutionOwnerUserId;
  }

  public Long getPmUserId() {
    return pmUserId;
  }

  public void setPmUserId(Long pmUserId) {
    this.pmUserId = pmUserId;
  }

  public Long getOpsOwnerUserId() {
    return opsOwnerUserId;
  }

  public void setOpsOwnerUserId(Long opsOwnerUserId) {
    this.opsOwnerUserId = opsOwnerUserId;
  }
}
