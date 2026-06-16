/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/roles}. */
public class RoleCreateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  private Boolean enabled;

  /** v0.0.20: grants the full admin console. Defaults to false when omitted. */
  private Boolean adminAccess;

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

  public Boolean getAdminAccess() {
    return adminAccess;
  }

  public void setAdminAccess(Boolean adminAccess) {
    this.adminAccess = adminAccess;
  }
}
