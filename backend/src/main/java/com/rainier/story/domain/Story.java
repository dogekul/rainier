/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * A Story is a PO-authored vertical slice of a {@code Requirement} — the management view of a
 * deliverable unit. Soft-deleted via {@link BaseEntity#delFlag}.
 *
 * <p>v0.0.9 invariants:
 *
 * <ul>
 *   <li>{@code requirementId} is NOT NULL (every Story belongs to a Requirement).
 *   <li>{@code projectId} is auto-copied from the parent Requirement at creation time (may be null
 *       if the parent Requirement has no project). NOT exposed on the create DTO.
 *   <li>{@code ownerUserId} is mutable (sibling pattern of v0.0.8 Project / Requirement).
 *   <li>Uniqueness on {@code code} is enforced at the service layer (no DB UNIQUE — family
 *       pattern).
 * </ul>
 *
 * <p>Task is a separate entity (not modeled here) — it can be user-created / system-scheduled /
 * AI-generated and is intentionally decoupled from Story.
 */
@Entity
@Table(name = "rainier_story")
@SQLDelete(
    sql = "UPDATE rainier_story SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Story extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String title;

  @Column(length = 4000)
  private String description;

  @Column(name = "acceptance_criteria", length = 4000)
  private String acceptanceCriteria;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false, length = 16)
  private String priority;

  @Column(length = 8)
  private String complexity;

  @Column(name = "requirement_id", nullable = false)
  private Long requirementId;

  /** Inherited from parent Requirement at creation. Nullable when parent has no Project. */
  @Column(name = "project_id")
  private Long projectId;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @Column(name = "close_reason", length = 500)
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

  public String getAcceptanceCriteria() {
    return acceptanceCriteria;
  }

  public void setAcceptanceCriteria(String acceptanceCriteria) {
    this.acceptanceCriteria = acceptanceCriteria;
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

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
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
