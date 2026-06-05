/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.dto;

/** Payload for {@code PUT /api/organizations/{id}/parent}. New parent or null for root. */
public class OrganizationMoveRequest {

  private Long parentId;

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }
}
