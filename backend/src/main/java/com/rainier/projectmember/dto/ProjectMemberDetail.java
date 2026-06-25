/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.dto;

import com.rainier.projectmember.domain.ProjectMember;
import java.time.Instant;
import java.util.List;

/**
 * v0.0.64 — Project member 读 DTO（含 enriched user 名 + displayLabel 区分合成 owner/pmo vs 真实成员）。
 *
 * <ul>
 *   <li>真实 ProjectMember row：id != null, role ∈ PD/DEV/.../OTHER, displayLabel = role 中文标签
 *   <li>合成 owner 行：id = null, role = "OWNER", displayLabel = "负责人"
 *   <li>合成 pmo 行：id = null, role = "PMO", displayLabel = "项目PMO"
 * </ul>
 */
public class ProjectMemberDetail {

  /** null on synthesized OWNER / PMO rows. */
  private Long id;

  private Long projectId;
  private Long userId;
  private String userName;
  private String userLoginName;
  private String role;
  private String displayLabel;
  private Instant joinedAt;
  private String joinedBy;

  /**
   * v0.0.88 (C8) — 项目内多角色（distinct, sorted）。包含 ProjectMember.role 本身（如果 ProjectMember
   * 有 role）。OWNER/PMO 合成行此处为单元素 [role]。
   */
  private List<String> roles;

  public static ProjectMemberDetail from(ProjectMember m) {
    ProjectMemberDetail d = new ProjectMemberDetail();
    d.id = m.getId();
    d.projectId = m.getProjectId();
    d.userId = m.getUserId();
    d.role = m.getRole();
    d.joinedAt = m.getJoinedAt();
    d.joinedBy = m.getJoinedBy();
    return d;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
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

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  public String getDisplayLabel() {
    return displayLabel;
  }

  public void setDisplayLabel(String displayLabel) {
    this.displayLabel = displayLabel;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  public String getJoinedBy() {
    return joinedBy;
  }

  public void setJoinedBy(String joinedBy) {
    this.joinedBy = joinedBy;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }
}
