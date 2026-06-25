/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.dto;

import javax.validation.constraints.NotBlank;

public class ProjectMemberUpdateRequest {

  @NotBlank private String role;

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }
}
