/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * A Story is a PO-authored vertical slice of a {@code Sprint}, which in turn belongs to a {@code
 * Requirement}. Story → Sprint → Requirement → Project hierarchy. Soft-deleted via {@link
 * BaseEntity#delFlag}.
 *
 * <p>v0.0.10 invariants:
 *
 * <ul>
 *   <li>{@code sprintId} is NOT NULL (every Story belongs to a Sprint). The DB column is upgraded
 *       to NOT NULL by {@code LegacyStoryToSprintMigration} Step 2 at first v0.0.10 startup; {@code
 *       columnDefinition="BIGINT"} lets Hibernate's first-boot ADD COLUMN land as nullable so
 *       existing rows can be migrated.
 *   <li>{@code projectId} is auto-copied from the parent Sprint's Requirement at creation time.
 *   <li>{@code ownerUserId} is mutable.
 *   <li>Uniqueness on {@code code} is enforced at the service layer (no DB UNIQUE).
 *   <li>{@code requirement_id} column is a legacy v0.0.9 artifact, no longer in the entity; the
 *       column was loosened to NULL by the migration to allow new inserts to omit it. v0.0.11+
 *       cleanup will DROP the column.
 * </ul>
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

  /**
   * v0.0.10: every Story belongs to exactly one Sprint. The DB column starts nullable (Hibernate
   * adds via {@code columnDefinition}) so the first-boot migration can fill existing rows; Step 2
   * of {@code LegacyStoryToSprintMigration} then runs ALTER MODIFY COLUMN to upgrade to NOT NULL.
   */
  @Column(name = "sprint_id", nullable = false, columnDefinition = "BIGINT")
  private Long sprintId;

  /**
   * Inherited from parent Sprint's Requirement at creation. Nullable when grandparent Requirement
   * has no Project.
   */
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

  public Long getSprintId() {
    return sprintId;
  }

  public void setSprintId(Long sprintId) {
    this.sprintId = sprintId;
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
