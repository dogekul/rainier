/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.dto;

import com.rainier.userorganization.domain.UserOrgRole;
import java.time.Instant;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/user-organizations}. */
public class UserOrgCreateRequest {

  @NotBlank
  @Size(max = 32)
  private String userId;

  @NotBlank
  @Size(max = 32)
  private String organizationId;

  private UserOrgRole role;
  private Boolean isPrimary;
  private Instant joinedAt;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
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
