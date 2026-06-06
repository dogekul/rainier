/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.dto;

import com.rainier.userrole.domain.UserRole;
import java.time.Instant;

/** Response DTO for {@link UserRole} read endpoints — enriched with user + role fields. */
public class UserRoleDetail {

  private Long id;
  private Long userId;
  private Long roleId;
  private Long projectId;
  private String userName;
  private String userLoginName;
  private String roleName;
  private String roleCode;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static UserRoleDetail from(UserRole ur) {
    UserRoleDetail dto = new UserRoleDetail();
    dto.id = ur.getId();
    dto.userId = ur.getUserId();
    dto.roleId = ur.getRoleId();
    dto.projectId = ur.getProjectId();
    dto.createTime = ur.getCreateTime();
    dto.updateTime = ur.getUpdateTime();
    dto.createBy = ur.getCreateBy();
    dto.updateBy = ur.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public Long getRoleId() {
    return roleId;
  }

  public Long getProjectId() {
    return projectId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserLoginName() {
    return userLoginName;
  }

  public void setUserLoginName(String userLoginName) {
    this.userLoginName = userLoginName;
  }

  public String getRoleName() {
    return roleName;
  }

  public void setRoleName(String roleName) {
    this.roleName = roleName;
  }

  public String getRoleCode() {
    return roleCode;
  }

  public void setRoleCode(String roleCode) {
    this.roleCode = roleCode;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public String getCreateBy() {
    return createBy;
  }

  public String getUpdateBy() {
    return updateBy;
  }
}
