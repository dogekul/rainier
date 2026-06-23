/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * A pre-sales opportunity (v0.0.44) — 客户全流程「售前环节」. Moves through {@link OpportunityStage}
 * (LEAD→CONTRACT) gated at 商机/投标/合同; outcome {@link OpportunityStatus} (OPEN/WON/LOST). On WON
 * (合同签订) it is initiated into a delivery {@code Project} via {@code projectId} (实施 = Project).
 */
@Entity
@Table(name = "rainier_opportunity")
@SQLDelete(
    sql = "UPDATE rainier_opportunity SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Opportunity extends BaseEntity {

  @Column(name = "customer_name", nullable = false, length = 100)
  private String customerName;

  @Column(nullable = false, length = 200)
  private String title;

  /** Deal value (元) — pipeline value / 赢单金额. Nullable. */
  @Column private Long amount;

  @Column(nullable = false, length = 16)
  private String stage = OpportunityStage.LEAD;

  @Column(nullable = false, length = 8)
  private String status = OpportunityStatus.OPEN;

  // Four phase owners (客户全流程图 4 负责人), all optional.
  @Column(name = "commercial_owner_user_id")
  private Long commercialOwnerUserId; // 商务

  @Column(name = "solution_owner_user_id")
  private Long solutionOwnerUserId; // 解决方案

  @Column(name = "pm_user_id")
  private Long pmUserId; // 项目经理

  @Column(name = "ops_owner_user_id")
  private Long opsOwnerUserId; // 运营经理

  /** Set at 立项 (initiate) once WON — links the delivery Project (实施 phase). */
  @Column(name = "project_id")
  private Long projectId;

  /** Who recorded the most recent gate decision (商机/投标/合同/立项). */
  @Column(name = "gate_decided_by", length = 32)
  private String gateDecidedBy;

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

  public String getStage() {
    return stage;
  }

  public void setStage(String stage) {
    this.stage = stage;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getGateDecidedBy() {
    return gateDecidedBy;
  }

  public void setGateDecidedBy(String gateDecidedBy) {
    this.gateDecidedBy = gateDecidedBy;
  }
}
