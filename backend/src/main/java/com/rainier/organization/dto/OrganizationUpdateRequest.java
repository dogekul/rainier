/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/organizations/{id}}. Updates mutable fields only. */
public class OrganizationUpdateRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  private Boolean isPmo;

  private Boolean enabled;

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

  public Boolean getIsPmo() {
    return isPmo;
  }

  public void setIsPmo(Boolean isPmo) {
    this.isPmo = isPmo;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
