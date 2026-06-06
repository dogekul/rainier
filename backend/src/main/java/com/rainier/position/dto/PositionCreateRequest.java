/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/positions}. */
public class PositionCreateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 500)
  private String description;

  @NotBlank
  @Size(max = 16)
  private String category;

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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
