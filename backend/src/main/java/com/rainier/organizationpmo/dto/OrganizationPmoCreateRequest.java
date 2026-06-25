/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.dto;

import javax.validation.constraints.NotNull;

public class OrganizationPmoCreateRequest {

  @NotNull private Long userId;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
