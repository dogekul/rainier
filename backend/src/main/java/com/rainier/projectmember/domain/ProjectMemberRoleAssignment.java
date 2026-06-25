/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.88 (C8) — 项目成员 ↔ 项目内角色 多对多关联表。
 *
 * <p>{@link ProjectMember} 仍保留 {@code role} 字段作为「首选 role」与向后兼容；本表承载多角色。
 * UNIQUE(project_member_id, project_role) 保证同一 member 不重复挂同一 role。
 * 软删除，便于 audit / undelete。
 *
 * <p>表名 {@code rainier_project_member_role}（spec 指定）。类名以 *Assignment 后缀避免与既有常量类
 * {@link ProjectMemberRole} 冲突。
 */
@Entity
@Table(
    name = "rainier_project_member_role",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_pmr_member_role",
            columnNames = {"project_member_id", "project_role"}))
@SQLDelete(
    sql =
        "UPDATE rainier_project_member_role SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class ProjectMemberRoleAssignment extends BaseEntity {

  @Column(name = "project_member_id", nullable = false)
  private Long projectMemberId;

  @Column(name = "project_role", nullable = false, length = 16)
  private String projectRole;

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
