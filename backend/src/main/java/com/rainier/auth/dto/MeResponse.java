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
  /** v0.0.69 (A5): 当前用户的 AI 授权级别 — BASIC / INTERMEDIATE / DEPTH，未设置默认 "BASIC"。 */
  private final String aiAuthLevel;

  /** v0.0.78 (B5): 该用户作为项目管理员的全部 projectId（可能为空 list，永不 null）。 */
  private final List<Long> adminProjectIds;

  public MeResponse(
      Long id, String username, String name, List<MeRole> roles, List<MeProject> projects) {
    this(
        id,
        username,
        name,
        roles,
        projects,
        "BASIC",
        java.util.Collections.<Long>emptyList(),
        "/");
  }

  public MeResponse(
      Long id,
      String username,
      String name,
      List<MeRole> roles,
      List<MeProject> projects,
      String aiAuthLevel) {
    this(id, username, name, roles, projects, aiAuthLevel, java.util.Collections.<Long>emptyList(), "/");
  }

  public MeResponse(
      Long id,
      String username,
      String name,
      List<MeRole> roles,
      List<MeProject> projects,
      String aiAuthLevel,
      List<Long> adminProjectIds) {
    this(id, username, name, roles, projects, aiAuthLevel, adminProjectIds, "/");
  }

  public MeResponse(
      Long id,
      String username,
      String name,
      List<MeRole> roles,
      List<MeProject> projects,
      String aiAuthLevel,
      List<Long> adminProjectIds,
      String defaultLandingPath) {
    this.id = id;
    this.username = username;
    this.name = name;
    this.roles = roles;
    this.projects = projects;
    this.aiAuthLevel = aiAuthLevel == null ? "BASIC" : aiAuthLevel;
    this.adminProjectIds =
        adminProjectIds == null ? java.util.Collections.<Long>emptyList() : adminProjectIds;
    this.defaultLandingPath =
        defaultLandingPath == null || defaultLandingPath.trim().isEmpty() ? "/" : defaultLandingPath;
  }

  /** v0.0.113 (H6): role/ownership-derived landing path used after login and cold reload. */
  private final String defaultLandingPath;

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

  public String getAiAuthLevel() {
    return aiAuthLevel;
  }

  public List<Long> getAdminProjectIds() {
    return adminProjectIds;
  }

  public String getDefaultLandingPath() {
    return defaultLandingPath;
  }

  /** One role assignment of the current user (role + the project it applies to, if any). */
  public static class MeRole {
    private final Long roleId;
    private final String roleCode;
    private final String roleName;
    private final Long projectId;
    private final String projectName;
    private final String projectCode;
    private final Boolean adminAccess;

    public MeRole(
        Long roleId,
        String roleCode,
        String roleName,
        Long projectId,
        String projectName,
        String projectCode,
        Boolean adminAccess) {
      this.roleId = roleId;
      this.roleCode = roleCode;
      this.roleName = roleName;
      this.projectId = projectId;
      this.projectName = projectName;
      this.projectCode = projectCode;
      this.adminAccess = adminAccess;
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

    /** v0.0.20: whether this role grants the full admin console (never null — false when unknown). */
    public Boolean getAdminAccess() {
      return adminAccess;
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
