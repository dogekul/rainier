/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Sprint is the management-layer decomposition between Requirement and Story (Requirement → Sprint
 * → Story). v0.0.10 semantics:
 *
 * <ul>
 *   <li>Hierarchical only — NOT an agile time-box; service does not enforce date coherence.
 *   <li>{@code requirementId} is NOT NULL (every Sprint belongs to exactly one Requirement).
 *   <li>{@code ownerUserId} is mutable (sibling of v0.0.8 Decision 6b).
 *   <li>code uniqueness is service-level (no DB UNIQUE — family pattern).
 *   <li>{@code start_date}, {@code end_date}, {@code goal} are reference metadata only.
 * </ul>
 */
@Entity
@Table(name = "rainier_sprint")
@SQLDelete(
    sql = "UPDATE rainier_sprint SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Sprint extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(length = 2000)
  private String goal;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "requirement_id", nullable = false)
  private Long requirementId;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

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

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }
}
