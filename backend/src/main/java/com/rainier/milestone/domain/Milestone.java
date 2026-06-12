/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Milestone is a project-scoped point-in-time checkpoint (e.g. 需求评审完成 / Beta发布 / 上线).
 *
 * <p>v0.0.17 semantics:
 *
 * <ul>
 *   <li>Distinct from {@code Sprint} (a time-boxed iteration) — a Milestone is a single dated point,
 *       not an interval; it holds no stories/tasks.
 *   <li>{@code projectId} is NOT NULL (every Milestone belongs to exactly one Project) and immutable.
 *   <li>{@code (projectId, code)} is unique at the service layer (no DB UNIQUE — soft-deleted codes
 *       reusable, same family pattern as Sprint/Project; different projects may share a code).
 *   <li>{@code status} freely changeable (no state machine), validated against {@code
 *       MilestoneStatus.ALL}.
 *   <li>Soft-deleted; cascade soft-deleted when the owning Project is deleted (see ProjectService).
 * </ul>
 */
@Entity
@Table(name = "rainier_milestone")
@SQLDelete(
    sql =
        "UPDATE rainier_milestone SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Milestone extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 2000)
  private String description;

  /** Planned date — the milestone's core attribute (NOT NULL). */
  @Column(name = "target_date", nullable = false)
  private LocalDate targetDate;

  @Column(nullable = false, length = 16)
  private String status;

  /** When it was actually reached (nullable until reached). */
  @Column(name = "actual_date")
  private LocalDate actualDate;

  /** Display order within the project; service coerces null→0 on create, so never null in practice. */
  @Column(name = "sort_order")
  private Integer sortOrder = 0;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate targetDate) {
    this.targetDate = targetDate;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDate getActualDate() {
    return actualDate;
  }

  public void setActualDate(LocalDate actualDate) {
    this.actualDate = actualDate;
  }

  public Integer getSortOrder() {
    return sortOrder;
  }

  public void setSortOrder(Integer sortOrder) {
    this.sortOrder = sortOrder;
  }
}
