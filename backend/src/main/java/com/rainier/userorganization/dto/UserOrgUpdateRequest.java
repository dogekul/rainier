/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.dto;

import com.rainier.userorganization.domain.UserOrgRole;
import java.time.Instant;

/** Payload for {@code PUT /api/user-organizations/{id}}. user_id/organization_id are immutable. */
public class UserOrgUpdateRequest {

  private String organizationId;
  private UserOrgRole role;
  private Boolean isPrimary;
  private Instant leftAt;

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

  public Instant getLeftAt() {
    return leftAt;
  }

  public void setLeftAt(Instant leftAt) {
    this.leftAt = leftAt;
  }
}
