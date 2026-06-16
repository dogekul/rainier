/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/roles/{id}}. {@code code} is immutable. */
public class RoleUpdateRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  private Boolean enabled;

  /** v0.0.20: grants the full admin console. When null, the existing value is kept. */
  private Boolean adminAccess;

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

  public Boolean getAdminAccess() {
    return adminAccess;
  }

  public void setAdminAccess(Boolean adminAccess) {
    this.adminAccess = adminAccess;
  }
}
