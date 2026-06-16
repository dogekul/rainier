/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Role pool — functional "hat" worn by a User in a project context (via {@link
 * com.rainier.userrole.domain.UserRole}).
 *
 * <p>NOTE: distinct from v0.0.3 {@code UserOrganization.role: UserOrgRole(HEAD/MEMBER)} which is an
 * org-hierarchy position; the two concepts coexist (see design.md decision 6).
 */
@Entity
@Table(name = "rainier_role")
@SQLDelete(
    sql = "UPDATE rainier_role SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Role extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false)
  private Boolean enabled = Boolean.TRUE;

  /**
   * v0.0.20: whether wearers of this role get the full admin console (vs. the plain user view of just
   * 工作台 + 需求管理). Nullable on purpose — adding a NOT NULL column to the existing rainier_role table
   * under ddl-auto=update would fail on legacy rows; the getter coalesces NULL → false instead.
   */
  @Column(name = "admin_access")
  private Boolean adminAccess = Boolean.FALSE;

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

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  /** Read-coalesce: a legacy NULL admin_access reads as false (non-admin). */
  public Boolean getAdminAccess() {
    return adminAccess == null ? Boolean.FALSE : adminAccess;
  }

  public void setAdminAccess(Boolean adminAccess) {
    this.adminAccess = adminAccess;
  }
}
