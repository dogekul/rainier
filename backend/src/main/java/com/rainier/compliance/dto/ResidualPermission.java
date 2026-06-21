/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.dto;

import java.util.List;

/**
 * A disabled user who still holds role grants (v0.0.41) — the「停用-残留权限」reconciliation row. The
 * grants are inert (a disabled user is never elevated and cannot authenticate) but should be revoked as
 * a de-provisioning hygiene step; this surfaces them for the admin.
 */
public class ResidualPermission {

  private final Long userId;
  private final String name;
  private final String loginName;
  private final int roleCount;
  private final List<String> roleNames;

  public ResidualPermission(
      Long userId, String name, String loginName, int roleCount, List<String> roleNames) {
    this.userId = userId;
    this.name = name;
    this.loginName = loginName;
    this.roleCount = roleCount;
    this.roleNames = roleNames;
  }

  public Long getUserId() {
    return userId;
  }

  public String getName() {
    return name;
  }

  public String getLoginName() {
    return loginName;
  }

  public int getRoleCount() {
    return roleCount;
  }

  public List<String> getRoleNames() {
    return roleNames;
  }
}
