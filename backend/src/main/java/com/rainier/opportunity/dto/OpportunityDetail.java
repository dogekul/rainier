/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import com.rainier.opportunity.domain.Opportunity;
import java.time.Instant;

/** Read DTO for {@link Opportunity} (v0.0.44), with owner-name enrichment. */
public class OpportunityDetail {

  private Long id;
  private String customerName;
  private String title;
  private String note;
  private Long amount;
  private String stage;
  private String status;
  private Long commercialOwnerUserId;
  private String commercialOwnerName;
  private Long solutionOwnerUserId;
  private String solutionOwnerName;
  private Long pmUserId;
  private String pmName;
  private Long opsOwnerUserId;
  private String opsOwnerName;
  private Long projectId;
  private Long productId;
  private String productName;
  private Long customerId;
  private String gateDecidedBy;
  private Instant createTime;
  private Instant updateTime;

  public static OpportunityDetail from(Opportunity o) {
    OpportunityDetail d = new OpportunityDetail();
    d.id = o.getId();
    d.customerName = o.getCustomerName();
    d.title = o.getTitle();
    d.note = o.getNote();
    d.amount = o.getAmount();
    d.stage = o.getStage();
    d.status = o.getStatus();
    d.commercialOwnerUserId = o.getCommercialOwnerUserId();
    d.solutionOwnerUserId = o.getSolutionOwnerUserId();
    d.pmUserId = o.getPmUserId();
    d.opsOwnerUserId = o.getOpsOwnerUserId();
    d.projectId = o.getProjectId();
    d.productId = o.getProductId();
    d.customerId = o.getCustomerId();
    d.gateDecidedBy = o.getGateDecidedBy();
    d.createTime = o.getCreateTime();
    d.updateTime = o.getUpdateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getTitle() {
    return title;
  }

  public String getNote() {
    return note;
  }

  public Long getAmount() {
    return amount;
  }

  public String getStage() {
    return stage;
  }

  public String getStatus() {
    return status;
  }

  public Long getCommercialOwnerUserId() {
    return commercialOwnerUserId;
  }

  public String getCommercialOwnerName() {
    return commercialOwnerName;
  }

  public void setCommercialOwnerName(String commercialOwnerName) {
    this.commercialOwnerName = commercialOwnerName;
  }

  public Long getSolutionOwnerUserId() {
    return solutionOwnerUserId;
  }

  public String getSolutionOwnerName() {
    return solutionOwnerName;
  }

  public void setSolutionOwnerName(String solutionOwnerName) {
    this.solutionOwnerName = solutionOwnerName;
  }

  public Long getPmUserId() {
    return pmUserId;
  }

  public String getPmName() {
    return pmName;
  }

  public void setPmName(String pmName) {
    this.pmName = pmName;
  }

  public Long getOpsOwnerUserId() {
    return opsOwnerUserId;
  }

  public String getOpsOwnerName() {
    return opsOwnerName;
  }

  public void setOpsOwnerName(String opsOwnerName) {
    this.opsOwnerName = opsOwnerName;
  }

  public Long getProjectId() {
    return projectId;
  }

  public Long getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public String getGateDecidedBy() {
    return gateDecidedBy;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }
}
