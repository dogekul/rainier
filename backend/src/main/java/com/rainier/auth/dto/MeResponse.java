/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.dto;

import java.util.List;

/**
 * Current-user context returned by {@code GET /api/auth/me}.
 *
 * <p>v0.0.18: expanded from {@code {username}} to the full identity + role/project context that the
 * frontend 我的工作台 needs. When the token subject does not resolve to a User (e.g. a mock "system"
 * principal), {@code id} is null and {@code roles}/{@code projects} are empty (graceful degrade).
 */
public class MeResponse {

  private final Long id;
  private final String username;
  private final String name;
  private final List<MeRole> roles;
  private final List<MeProject> projects;

  public MeResponse(
      Long id, String username, String name, List<MeRole> roles, List<MeProject> projects) {
    this.id = id;
    this.username = username;
    this.name = name;
    this.roles = roles;
    this.projects = projects;
  }

  public Long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public String getName() {
    return name;
  }

  public List<MeRole> getRoles() {
    return roles;
  }

  public List<MeProject> getProjects() {
    return projects;
  }

  /** One role assignment of the current user (role + the project it applies to, if any). */
  public static class MeRole {
    private final Long roleId;
    private final String roleCode;
    private final String roleName;
    private final Long projectId;
    private final String projectName;
    private final String projectCode;

    public MeRole(
        Long roleId,
        String roleCode,
        String roleName,
        Long projectId,
        String projectName,
        String projectCode) {
      this.roleId = roleId;
      this.roleCode = roleCode;
      this.roleName = roleName;
      this.projectId = projectId;
      this.projectName = projectName;
      this.projectCode = projectCode;
    }

    public Long getRoleId() {
      return roleId;
    }

    public String getRoleCode() {
      return roleCode;
    }

    public String getRoleName() {
      return roleName;
    }

    public Long getProjectId() {
      return projectId;
    }

    public String getProjectName() {
      return projectName;
    }

    public String getProjectCode() {
      return projectCode;
    }
  }

  /** A project the current user participates in (distinct across role assignments). */
  public static class MeProject {
    private final Long id;
    private final String code;
    private final String name;

    public MeProject(Long id, String code, String name) {
      this.id = id;
      this.code = code;
      this.name = name;
    }

    public Long getId() {
      return id;
    }

    public String getCode() {
      return code;
    }

    public String getName() {
      return name;
    }
  }
}
