/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.dto;

import com.rainier.userorganization.domain.UserOrgRole;
import java.time.Instant;
import javax.validation.constraints.NotNull;

/** Payload for {@code POST /api/user-organizations}. */
public class UserOrgCreateRequest {

  @NotNull private Long userId;

  @NotNull private Long organizationId;

  private UserOrgRole role;
  private Boolean isPrimary;
  private Instant joinedAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public UserOrgRole getRole() {
    return role;
  }

  public void setRole(UserOrgRole role) {
    this.role = role;
  }

  public Boolean getIsPrimary() {
    return isPrimary;
  }

  public void setIsPrimary(Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }
}
