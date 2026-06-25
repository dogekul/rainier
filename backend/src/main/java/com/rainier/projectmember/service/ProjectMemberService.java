/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.domain.ProjectMemberRole;
import com.rainier.projectmember.dto.ProjectMemberCreateRequest;
import com.rainier.projectmember.dto.ProjectMemberDetail;
import com.rainier.projectmember.dto.ProjectMemberUpdateRequest;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.64 — 项目成员业务（add / updateRole / delete / listMembers，含合成 owner+pmo 行）。
 *
 * <p>create/update/delete 命名让 AuditAspect 自动捕获写入审计日志（与项目惯例一致）。
 */
@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

  private final ProjectMemberRepository repo;
  private final ProjectRepository projectRepo;
  private final UserRepository userRepo;
  private final AuditorAware<String> auditorAware;

  public ProjectMemberService(
      ProjectMemberRepository repo,
      ProjectRepository projectRepo,
      UserRepository userRepo,
      AuditorAware<String> auditorAware) {
    this.repo = repo;
    this.projectRepo = projectRepo;
    this.userRepo = userRepo;
    this.auditorAware = auditorAware;
  }

  @Transactional
  public ProjectMemberDetail create(Long projectId, ProjectMemberCreateRequest req) {
    Project project =
        projectRepo
            .findById(projectId)
            .orElseThrow(() -> new NotFoundException("project not found: id=" + projectId));
    String role = req.getRole();
    if (role == null || !ProjectMemberRole.ALL.contains(role)) {
      throw new BadRequestException("invalid role: " + role);
    }
    if (!userRepo.existsById(req.getUserId())) {
      throw new BadRequestException("user not found: id=" + req.getUserId());
    }
    if (req.getUserId().equals(project.getOwnerUserId())) {
      throw new BadRequestException("该用户已是项目负责人");
    }
    if (repo.existsByProjectIdAndUserId(projectId, req.getUserId())) {
      throw new ConflictException("已是项目成员");
    }
    ProjectMember m = new ProjectMember();
    m.setProjectId(projectId);
    m.setUserId(req.getUserId());
    m.setRole(role);
    m.setJoinedAt(Instant.now());
    m.setJoinedBy(auditorAware.getCurrentAuditor().orElse("system"));
    ProjectMember saved = repo.saveAndFlush(m);
    return enrich(saved, null);
  }

  @Transactional
  public ProjectMemberDetail update(Long projectId, Long userId, ProjectMemberUpdateRequest req) {
    ProjectMember m =
        repo.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new NotFoundException("project member not found"));
    String role = req.getRole();
    if (role == null || !ProjectMemberRole.ALL.contains(role)) {
      throw new BadRequestException("invalid role: " + role);
    }
    m.setRole(role);
    ProjectMember saved = repo.saveAndFlush(m);
    return enrich(saved, null);
  }

  @Transactional
  public void delete(Long projectId, Long userId) {
    Project project =
        projectRepo
            .findById(projectId)
            .orElseThrow(() -> new NotFoundException("project not found: id=" + projectId));
    if (userId.equals(project.getOwnerUserId())) {
      throw new BadRequestException("不可移除负责人");
    }
    ProjectMember m =
        repo.findByProjectIdAndUserId(projectId, userId)
            .orElseThrow(() -> new NotFoundException("project member not found"));
    repo.delete(m);
  }

  /**
   * v0.0.64 — list members of a project. Order:
   *
   * <ol>
   *   <li>OWNER 合成行（必有）
   *   <li>PMO 合成行（仅当 project.pmoUserId != null && != ownerUserId）
   *   <li>真实 ProjectMember rows 按 joined_at DESC
   * </ol>
   */
  public List<ProjectMemberDetail> listMembers(Long projectId) {
    Project project =
        projectRepo
            .findById(projectId)
            .orElseThrow(() -> new NotFoundException("project not found: id=" + projectId));
    List<ProjectMember> rows = repo.findByProjectIdOrderByJoinedAtDesc(projectId);

    // Collect all userIds to enrich in one go.
    Set<Long> userIds = new HashSet<Long>();
    if (project.getOwnerUserId() != null) userIds.add(project.getOwnerUserId());
    if (project.getPmoUserId() != null) userIds.add(project.getPmoUserId());
    for (ProjectMember m : rows) userIds.add(m.getUserId());
    Map<Long, User> users =
        userRepo.findAllById(userIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

    List<ProjectMemberDetail> result = new ArrayList<ProjectMemberDetail>(rows.size() + 2);

    if (project.getOwnerUserId() != null) {
      result.add(synthRow(projectId, project.getOwnerUserId(), ProjectMemberRole.OWNER, "负责人",
          project.getCreateTime(), users));
    }
    if (project.getPmoUserId() != null
        && !project.getPmoUserId().equals(project.getOwnerUserId())) {
      result.add(synthRow(projectId, project.getPmoUserId(), ProjectMemberRole.PMO, "项目PMO",
          project.getCreateTime(), users));
    }
    for (ProjectMember m : rows) {
      // Defensive: skip if the real row's user equals owner/pmo (UNIQUE 不变量保证不应发生).
      if (m.getUserId().equals(project.getOwnerUserId())) continue;
      if (project.getPmoUserId() != null && m.getUserId().equals(project.getPmoUserId())) continue;
      result.add(enrich(m, users));
    }
    return result;
  }

  // ---------------------------- helpers ----------------------------

  private ProjectMemberDetail synthRow(
      Long projectId, Long userId, String role, String displayLabel, Instant joinedAt,
      Map<Long, User> users) {
    ProjectMemberDetail d = new ProjectMemberDetail();
    d.setId(null);
    d.setProjectId(projectId);
    d.setUserId(userId);
    d.setRole(role);
    d.setDisplayLabel(displayLabel);
    d.setJoinedAt(joinedAt);
    User u = users.get(userId);
    if (u != null) {
      d.setUserName(u.getName());
      d.setUserLoginName(u.getLoginName());
    }
    return d;
  }

  private ProjectMemberDetail enrich(ProjectMember m, Map<Long, User> usersOrNull) {
    ProjectMemberDetail d = ProjectMemberDetail.from(m);
    d.setDisplayLabel(roleLabel(m.getRole()));
    User u;
    if (usersOrNull != null && usersOrNull.containsKey(m.getUserId())) {
      u = usersOrNull.get(m.getUserId());
    } else {
      u = userRepo.findById(m.getUserId()).orElse(null);
    }
    if (u != null) {
      d.setUserName(u.getName());
      d.setUserLoginName(u.getLoginName());
    }
    return d;
  }

  private static String roleLabel(String role) {
    if (role == null) return "成员";
    if (ProjectMemberRole.PD.equals(role)) return "产品经理";
    if (ProjectMemberRole.DEV.equals(role)) return "研发";
    if (ProjectMemberRole.QA.equals(role)) return "测试";
    if (ProjectMemberRole.DESIGN.equals(role)) return "设计";
    if (ProjectMemberRole.BIZ.equals(role)) return "业务";
    if (ProjectMemberRole.OPS.equals(role)) return "运维";
    if (ProjectMemberRole.OTHER.equals(role)) return "其他";
    return role;
  }
}
