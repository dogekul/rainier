/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * {@code productId} omitted — immutable after creation; Jackson silently drops it.
 *
 * <p>{@code parentId} included since v0.0.13 — full-replace semantics: null (or omitted) moves the
 * module to top level; non-null reparents under strict validation (same product → no cycle →
 * depth cap).
 */
public class ProductModuleUpdateRequest {

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

  private Long parentId;

  @NotNull private Long ownerUserId;

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
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
