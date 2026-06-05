/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/organizations/{id}}. Updates mutable fields only. */
public class OrganizationUpdateRequest {

  /**
   * Optional. When omitted or unchanged the existing code is kept; when supplied with a new value
   * the service revalidates {@code (parent_id, code)} uniqueness.
   */
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  private Boolean enabled;

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
}
