/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class ProjectMemberCreateRequest {

  @NotNull private Long userId;

  @NotBlank private String role;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
