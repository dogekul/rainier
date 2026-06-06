/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * M2M link — user × role × project. Hard delete (no {@code @SQLDelete}).
 *
 * <p>{@code project_id} is a placeholder column (nullable, no FK constraint, no service-level
 * existence check) — pattern reused from v0.0.6 {@code Requirement.project_id}. v0.0.8 introducing
 * Project entity will retrofit validation and clean placeholder values.
 *
 * <p>MySQL UNIQUE allows multiple NULL rows on {@code project_id}; service layer adds an IS NULL
 * branch to cover that edge.
 */
@Entity
@Table(
    name = "rainier_user_role",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_role_user_role_project",
            columnNames = {"user_id", "role_id", "project_id"}))
public class UserRole extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "project_id")
  private Long projectId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
}
