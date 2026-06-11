/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Top-level PM container. Soft-deleted via {@link BaseEntity#delFlag}.
 *
 * <p>All "project-dimensional role hats" (PM / PMO / TechLead / ...) live in {@code
 * com.rainier.userrole.domain.UserRole} (user × role × project). Project entity holds ONLY {@code
 * ownerUserId} — the project lead / creator, an entity-essential attribute, not a hat.
 *
 * <p>{@code ownerUserId} IS mutable (unlike v0.0.6 {@code Requirement.ownerUserId} pre-v0.0.8 —
 * v0.0.8 also retrofits Requirement.owner to mutable; see entity-requirement MODIFIED spec).
 *
 * <p>Uniqueness on {@code code} is enforced at the service layer (no DB UNIQUE — same pattern as
 * Requirement.code / Position.code).
 */
@Entity
@Table(name = "rainier_project")
@SQLDelete(
    sql =
        "UPDATE rainier_project SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Project extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Column(nullable = false)
  private Boolean enabled = Boolean.TRUE;

  /**
   * v0.0.16 — extensible project type (CASUAL/FORMAL seed; see {@link ProjectType}). Nullable column
   * (so the ALTER is safe on existing MySQL rows); legacy NULLs are backfilled to CASUAL by {@code
   * ProjectTypeBackfill} at startup and read-coalesced in {@code ProjectDetail.from}.
   */
  @Column(name = "project_type", length = 16)
  private String projectType;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public String getProjectType() {
    return projectType;
  }

  public void setProjectType(String projectType) {
    this.projectType = projectType;
  }
}
