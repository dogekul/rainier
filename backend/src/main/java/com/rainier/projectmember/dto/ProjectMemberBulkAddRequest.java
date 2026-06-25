/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.dto;

import java.util.List;
import javax.validation.constraints.NotEmpty;

/**
 * v0.0.88 (C8) — bulk add 项目成员入参。
 *
 * <p>语义：笛卡尔积展开 —— 每个 userId 都获得 projectRoles 全集；service 内部对已存在成员做 merge。
 */
public class ProjectMemberBulkAddRequest {

  @NotEmpty private List<Long> memberUserIds;

  @NotEmpty private List<String> projectRoles;

  public List<Long> getMemberUserIds() {
    return memberUserIds;
  }

  public void setMemberUserIds(List<Long> memberUserIds) {
    this.memberUserIds = memberUserIds;
  }

  public List<String> getProjectRoles() {
    return projectRoles;
  }

  public void setProjectRoles(List<String> projectRoles) {
    this.projectRoles = projectRoles;
  }
}
