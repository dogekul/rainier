/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.dto;

import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/organizations/{id}/parent}. New parent or null for root. */
public class OrganizationMoveRequest {

  @Size(max = 32)
  private String parentId;

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }
}
