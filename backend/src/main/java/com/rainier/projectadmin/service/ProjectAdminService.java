/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectadmin.service;

import com.rainier.common.exception.NotFoundException;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.domain.ProjectMemberRole;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.78 (B5) — 项目级 admin 单标志位的 business helper.
 *
 * <p>地基方案：ProjectMember.isProjectAdmin Boolean 单字段；本服务封装 grant/revoke + 三个读查询。
 * 写方法命名用 {@code updateGrant}/{@code updateRevoke}，让 {@code AuditAspect}（匹配 create/update/delete
 * 前缀）自动捕获审计。
 */
@Service
@Transactional(readOnly = true)
public class ProjectAdminService {

  private final ProjectMemberRepository memberRepo;
  private final ProjectRepository projectRepo;
  private final UserRepository userRepo;

  public ProjectAdminService(
      ProjectMemberRepository memberRepo,
      ProjectRepository projectRepo,
      UserRepository userRepo) {
    this.memberRepo = memberRepo;
    this.projectRepo = projectRepo;
    this.userRepo = userRepo;
  }

  public boolean isProjectAdmin(Long userId, Long projectId) {
    if (userId == null || projectId == null) {
      return false;
    }
    Optional<ProjectMember> m = memberRepo.findByProjectIdAndUserId(projectId, userId);
    return m.isPresent() && Boolean.TRUE.equals(m.get().getIsProjectAdmin());
  }

  public List<Long> listProjectAdmins(Long projectId) {
    List<ProjectMember> rows = memberRepo.findByProjectIdAndIsProjectAdminTrue(projectId);
    List<Long> out = new ArrayList<Long>(rows.size());
    for (ProjectMember m : rows) {
      out.add(m.getUserId());
    }
    return out;
  }

  public List<Long> listAdminProjects(Long userId) {
    if (userId == null) {
      return new ArrayList<Long>();
    }
    List<ProjectMember> rows = memberRepo.findByUserIdAndIsProjectAdminTrue(userId);
    List<Long> out = new ArrayList<Long>(rows.size());
    for (ProjectMember m : rows) {
      out.add(m.getProjectId());
    }
    return out;
  }

  /**
   * v0.0.78 — Grant project-admin to userId for projectId. Idempotent: 已是 admin 静默返回当前列表。
   *
   * <p>若该用户在该项目中没有 ProjectMember 行，先建一行（role=OTHER）。owner / pmo 也可以被授为项目 admin
   * （他们本身就有项目级写权限，授后只是统一在 isProjectAdmin 上可查）。
   */
  @Transactional
  public List<Long> updateGrant(Long projectId, Long userId, String byUsername) {
    if (!projectRepo.existsById(projectId)) {
      throw new NotFoundException("project not found: id=" + projectId);
    }
    if (!userRepo.existsById(userId)) {
      throw new NotFoundException("user not found: id=" + userId);
    }
    Optional<ProjectMember> existing = memberRepo.findByProjectIdAndUserId(projectId, userId);
    ProjectMember m;
    if (existing.isPresent()) {
      m = existing.get();
    } else {
      m = new ProjectMember();
      m.setProjectId(projectId);
      m.setUserId(userId);
      m.setRole(ProjectMemberRole.OTHER);
      m.setJoinedAt(Instant.now());
      m.setJoinedBy(byUsername == null ? "system" : byUsername);
    }
    m.setIsProjectAdmin(Boolean.TRUE);
    memberRepo.saveAndFlush(m);
    return listProjectAdmins(projectId);
  }

  /**
   * v0.0.78 — Revoke project-admin. 幂等：行不存在直接 200。不删 ProjectMember 行（仅置 flag）。
   */
  @Transactional
  public List<Long> updateRevoke(Long projectId, Long userId) {
    if (!projectRepo.existsById(projectId)) {
      throw new NotFoundException("project not found: id=" + projectId);
    }
    Optional<ProjectMember> existing = memberRepo.findByProjectIdAndUserId(projectId, userId);
    if (existing.isPresent()) {
      ProjectMember m = existing.get();
      m.setIsProjectAdmin(Boolean.FALSE);
      memberRepo.saveAndFlush(m);
    }
    return listProjectAdmins(projectId);
  }
}
