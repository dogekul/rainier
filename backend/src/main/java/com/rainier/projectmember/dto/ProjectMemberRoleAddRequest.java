/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.dto;

import javax.validation.constraints.NotBlank;

/** v0.0.88 (C8) — 给已有 ProjectMember 追加一个 project role. */
public class ProjectMemberRoleAddRequest {

  @NotBlank private String projectRole;

  public String getProjectRole() {
    return projectRole;
  }

  public void setProjectRole(String projectRole) {
    this.projectRole = projectRole;
  }
}
