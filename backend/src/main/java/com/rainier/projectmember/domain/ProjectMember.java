/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.64 — 项目成员关系（Project ↔ User，含项目内 role 枚举）。
 *
 * <p>UNIQUE(project_id, user_id, del_flag) — 一人一项目一行；软删除后可重新加同一人。
 * Role ∈ {PD, DEV, QA, DESIGN, BIZ, OPS, OTHER}（见 {@link ProjectMemberRole}）。
 *
 * <p>owner / project.pmoUserId 不冗余进本表；read 时由 ProjectMemberService.listMembers 通过 UNION 合成
 * 「OWNER」「PMO」两条虚拟行置顶。
 */
@Entity
@Table(name = "rainier_project_member")
@SQLDelete(
    sql =
        "UPDATE rainier_project_member SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class ProjectMember extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false, length = 16)
  private String role;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  @Column(name = "joined_by", length = 32)
  private String joinedBy;

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

  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
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
}
