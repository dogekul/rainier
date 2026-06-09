/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Product update payload.
 *
 * <p>{@code categoryId} is intentionally absent — it is immutable after creation (spec Decision
 * 11 sibling). Any incoming {@code categoryId} field in the JSON is silently ignored by Jackson.
 */
public class ProductUpdateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 200)
  private String name;

  @Size(max = 4000)
  private String description;

  @NotBlank
  @Size(max = 16)
  private String status;

  @NotNull private Long ownerUserId;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }
}
