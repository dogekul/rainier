/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * An after-sales operation engagement (v0.0.44) — 客户全流程「售后环节」. Linear stages {@link
 * OperationStage} (回款维保→运营→复购); status ACTIVE/CLOSED. Links the delivered {@code Project} via
 * {@code projectId}.
 */
@Entity
@Table(name = "rainier_operation")
@SQLDelete(
    sql = "UPDATE rainier_operation SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Operation extends BaseEntity {

  public static final String ACTIVE = "ACTIVE";
  public static final String CLOSED = "CLOSED";

  @Column(name = "customer_name", nullable = false, length = 100)
  private String customerName;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(nullable = false, length = 16)
  private String stage = OperationStage.MAINTENANCE;

  @Column(nullable = false, length = 8)
  private String status = ACTIVE;

  @Column(name = "ops_owner_user_id")
  private Long opsOwnerUserId;

  @Column(name = "project_id")
  private Long projectId;

  /** v0.0.58 — 来源商机（可空，可追溯由哪条商机验收时自动建）。 */
  @Column(name = "opportunity_id")
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
