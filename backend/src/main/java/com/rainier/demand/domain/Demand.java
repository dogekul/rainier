/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Business stakeholder's raw demand. Soft-deleted via {@link BaseEntity#delFlag} (@SQLDelete
 * + @Where below).
 *
 * <p>AI fields {@code aiClassification} and {@code aiDuplicateHint} are placeholders (decision 3 in
 * design.md); the service layer does not accept client writes to them, only AI workers will (in a
 * future change).
 */
@Entity
@Table(name = "rainier_demand")
@SQLDelete(
    sql = "UPDATE rainier_demand SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Demand extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String title;

  @Column(length = 2000)
  private String description;

  @Column(name = "submitter_user_id", nullable = false)
  private Long submitterUserId;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false, length = 16)
  private String priority;

  @Column(nullable = false, length = 16)
  private String source;

  @Column(name = "ai_classification", length = 100)
  private String aiClassification;

  @Column(name = "ai_duplicate_hint")
  private Long aiDuplicateHint;

  @Column(name = "close_reason", length = 500)
  private String closeReason;

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

  public Long getSubmitterUserId() {
    return submitterUserId;
  }

  public void setSubmitterUserId(Long submitterUserId) {
    this.submitterUserId = submitterUserId;
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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getAiClassification() {
    return aiClassification;
  }

  public void setAiClassification(String aiClassification) {
    this.aiClassification = aiClassification;
  }

  public Long getAiDuplicateHint() {
    return aiDuplicateHint;
  }

  public void setAiDuplicateHint(Long aiDuplicateHint) {
    this.aiDuplicateHint = aiDuplicateHint;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }
}
