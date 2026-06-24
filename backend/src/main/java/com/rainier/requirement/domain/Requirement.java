/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * PO's translated requirement. Soft-deleted via {@link BaseEntity#delFlag}.
 *
 * <p>The {@code project_id} column is a placeholder FK — present so future Project entities can
 * link without schema change; not validated, no FK constraint in DB.
 */
@Entity
@Table(name = "rainier_requirement")
@SQLDelete(
    sql =
        "UPDATE rainier_requirement SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id"
            + " = ?")
@Where(clause = "del_flag = 0")
public class Requirement extends BaseEntity {

  // Uniqueness enforced at service layer (matches Organization.code pattern); leaving the DB
  // unique constraint off avoids soft-delete residue (del_flag=1 rows would otherwise block legal
  // re-insertion under the same code after a soft delete).
  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(length = 4000)
  private String description;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false, length = 16)
  private String priority;

  @Column(length = 8)
  private String complexity;

  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "close_reason", length = 500)
  private String closeReason;

  /** v0.0.19 — 期望交付日期 (nullable). */
  @Column(name = "expected_date")
  private LocalDate expectedDate;

  /** v0.0.56 — 来源商机（可空，可追溯由哪个商机的现场调研生成）。 */
  @Column(name = "opportunity_id")
  private Long opportunityId;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

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

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
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

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }

  public LocalDate getExpectedDate() {
    return expectedDate;
  }

  public void setExpectedDate(LocalDate expectedDate) {
    this.expectedDate = expectedDate;
  }
}
