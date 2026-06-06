/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.dto;

import javax.validation.constraints.NotNull;

/** Payload for {@code POST /api/user-roles}. {@code projectId} is optional and not validated. */
public class UserRoleCreateRequest {

  @NotNull private Long userId;

  @NotNull private Long roleId;

  /** Placeholder FK to future Project entity. v0 accepts any BIGINT or null. */
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
