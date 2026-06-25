/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.dto;

import com.rainier.projectmember.domain.ProjectMemberRoleAssignment;

/** v0.0.88 (C8) — ProjectMemberRoleAssignment 读 DTO. */
public class ProjectMemberRoleAssignmentDetail {

  private Long id;
  private Long projectMemberId;
  private String projectRole;

  public static ProjectMemberRoleAssignmentDetail from(ProjectMemberRoleAssignment a) {
    ProjectMemberRoleAssignmentDetail d = new ProjectMemberRoleAssignmentDetail();
    d.id = a.getId();
    d.projectMemberId = a.getProjectMemberId();
    d.projectRole = a.getProjectRole();
    return d;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProjectMemberId() {
    return projectMemberId;
  }

  public void setProjectMemberId(Long projectMemberId) {
    this.projectMemberId = projectMemberId;
  }

  public String getProjectRole() {
    return projectRole;
  }

  public void setProjectRole(String projectRole) {
    this.projectRole = projectRole;
  }
}
