/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.dto;

import com.rainier.organization.domain.OrganizationType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/organizations}. */
public class OrganizationCreateRequest {

  private Long parentId;

  @NotNull private OrganizationType type;

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  private Boolean enabled;

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public OrganizationType getType() {
    return type;
  }

  public void setType(OrganizationType type) {
    this.type = type;
  }

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
