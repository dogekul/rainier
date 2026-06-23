/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/opportunities} (v0.0.44). Created at stage=LEAD / status=OPEN. */
public class OpportunityCreateRequest {

  @NotBlank
  @Size(max = 100)
  private String customerName;

  @NotBlank
  @Size(max = 200)
  private String title;

  @Size(max = 2000)
  private String note;

  private Long amount;
  private Long commercialOwnerUserId;
  private Long solutionOwnerUserId;
  private Long pmUserId;
  private Long opsOwnerUserId;
  private Long productId;
  /** Optional 客户 link: pick an existing customer; if null, the customerName above creates one. */
  private Long customerId;

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

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
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

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }
}
