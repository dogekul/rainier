/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * v0.0.77 B4 — 角色 ↔ 权限点 M2M。Hard delete（无 {@code @SQLDelete}），del_flag 列继承自 BaseEntity 但不使用。
 *
 * <p>{@code permission_point} 存的是 {@link com.rainier.authz.PermissionPoint#name()}；常量名稳定后不要重命名。
 */
@Entity
@Table(
    name = "rainier_role_permission",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_role_permission_role_point",
            columnNames = {"role_id", "permission_point"}))
public class RolePermission extends BaseEntity {

  @Column(name = "role_id", nullable = false)
  private Long roleId;

  @Column(name = "permission_point", nullable = false, length = 64)
  private String permissionPoint;

  public Long getRoleId() {
    return roleId;
  }

  public void setRoleId(Long roleId) {
    this.roleId = roleId;
  }

  public String getPermissionPoint() {
    return permissionPoint;
  }

  public void setPermissionPoint(String permissionPoint) {
    this.permissionPoint = permissionPoint;
  }
}
