/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.domain.ProjectMemberRole;
import com.rainier.projectmember.domain.ProjectMemberRoleAssignment;
import com.rainier.projectmember.dto.ProjectMemberBulkAddRequest;
import com.rainier.projectmember.dto.ProjectMemberCreateRequest;
import com.rainier.projectmember.dto.ProjectMemberDetail;
import com.rainier.projectmember.dto.ProjectMemberRoleAssignmentDetail;
import com.rainier.projectmember.dto.ProjectMemberUpdateRequest;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.projectmember.repository.ProjectMemberRoleAssignmentRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.64 — 项目成员业务（add / updateRole / delete / listMembers，含合成 owner+pmo 行）。
 *
 * <p>v0.0.88 (C8) — 多角色 + bulk add：
 *
 * <ul>
 *   <li>{@link #bulkAdd} 一次加多人 × 多 role（笛卡尔积，已存在 member 走 merge）
 *   <li>{@link #addRoleToMember} / {@link #removeRoleFromMember} / {@link #listRolesOfMember}
 *   <li>read 路径富化 {@code roles[]}（含 ProjectMember.role 自身 + RoleAssignment 表）
 * </ul>
 *
 * <p>create/update/delete 命名让 AuditAspect 自动捕获写入审计日志（与项目惯例一致）。
 */
@Service
@Transactional(readOnly = true)
public class ProjectMemberService {

  private final ProjectMemberRepository repo;
  private final ProjectMemberRoleAssignmentRepository roleRepo;
  private final ProjectRepository projectRepo;
  private final UserRepository userRepo;
  private final AuditorAware<String> auditorAware;

  public ProjectMemberService(
      ProjectMemberRepository repo,
      ProjectMemberRoleAssignmentRepository roleRepo,
      ProjectRepository projectRepo,
      UserRepository userRepo,
      AuditorAware<String> auditorAware) {
    this.repo = repo;
    this.roleRepo = roleRepo;
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
    // v0.0.88 (C8) — 同步写 RoleAssignment（保证 roles[] 富化非空）
    persistRoleIfAbsent(saved.getId(), role);
    return enrich(saved, null, null);
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
    // v0.0.88 (C8) — keep RoleAssignment in sync（追加，不覆盖；删除走显式 removeRoleFromMember）
    persistRoleIfAbsent(saved.getId(), role);
    return enrich(saved, null, null);
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
    // v0.0.88 (C8) — cascade soft-delete role assignments
    List<ProjectMemberRoleAssignment> assigns = roleRepo.findByProjectMemberId(m.getId());
    if (!assigns.isEmpty()) {
      roleRepo.deleteAll(assigns);
    }
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
   *
   * <p>v0.0.88 (C8) — 富化 {@code roles[]}（合成行单元素；真实行从 RoleAssignment 表取）.
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

    // v0.0.88 (C8) — batch fetch all role assignments for these members
    Map<Long, List<String>> rolesByMemberId = loadRolesByMemberId(rows);

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
      result.add(enrich(m, users, rolesByMemberId));
    }
    return result;
  }

  // ============================== v0.0.88 (C8) bulk + multi-role ==============================

  /**
   * v0.0.88 (C8) — bulk add 项目成员 + 多角色。
   *
   * <p>语义：笛卡尔积（每个 userId 都获得 projectRoles 全集）。
   *
   * <ul>
   *   <li>userId == owner → 静默跳过（不抛 400）
   *   <li>userId 已是 member → 在其 PM 上 merge 缺失的 role assignments（idempotent）
   *   <li>userId 新成员 → 新建 PM (role = projectRoles[0]) + 每个 role 一条 assignment
   *   <li>任一 role 非法 → 400（fail-fast，不部分提交）
   * </ul>
   *
   * @return 每个 userId 一行（含 owner-skip 后剩余的 user）
   */
  @Transactional
  public List<ProjectMemberDetail> bulkAdd(Long projectId, ProjectMemberBulkAddRequest req) {
    if (req.getMemberUserIds() == null || req.getMemberUserIds().isEmpty()) {
      throw new BadRequestException("memberUserIds 不能为空");
    }
    if (req.getProjectRoles() == null || req.getProjectRoles().isEmpty()) {
      throw new BadRequestException("projectRoles 不能为空");
    }
    Project project =
        projectRepo
            .findById(projectId)
            .orElseThrow(() -> new NotFoundException("project not found: id=" + projectId));

    // distinct + 顺序保留
    LinkedHashSet<String> roles = new LinkedHashSet<String>();
    for (String r : req.getProjectRoles()) {
      if (r == null || !ProjectMemberRole.ALL.contains(r)) {
        throw new BadRequestException("invalid role: " + r);
      }
      roles.add(r);
    }
    LinkedHashSet<Long> userIds = new LinkedHashSet<Long>(req.getMemberUserIds());

    String defaultRole = roles.iterator().next();
    String actor = auditorAware.getCurrentAuditor().orElse("system");
    Instant now = Instant.now();

    List<ProjectMember> touched = new ArrayList<ProjectMember>();
    for (Long uid : userIds) {
      if (uid == null) continue;
      if (uid.equals(project.getOwnerUserId())) {
        // owner already implicit；skip silently
        continue;
      }
      if (!userRepo.existsById(uid)) {
        throw new BadRequestException("user not found: id=" + uid);
      }
      ProjectMember m =
          repo.findByProjectIdAndUserId(projectId, uid)
              .orElse(null);
      if (m == null) {
        m = new ProjectMember();
        m.setProjectId(projectId);
        m.setUserId(uid);
        m.setRole(defaultRole);
        m.setJoinedAt(now);
        m.setJoinedBy(actor);
        m = repo.saveAndFlush(m);
      }
      for (String r : roles) {
        persistRoleIfAbsent(m.getId(), r);
      }
      touched.add(m);
    }

    // 富化（批量 user + roles）
    Set<Long> uids = new HashSet<Long>();
    for (ProjectMember m : touched) uids.add(m.getUserId());
    Map<Long, User> users =
        userRepo.findAllById(uids).stream()
            .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    Map<Long, List<String>> rolesByMemberId = loadRolesByMemberId(touched);

    List<ProjectMemberDetail> result = new ArrayList<ProjectMemberDetail>(touched.size());
    for (ProjectMember m : touched) {
      result.add(enrich(m, users, rolesByMemberId));
    }
    return result;
  }

  /** v0.0.88 (C8) — 给已有 ProjectMember 追加一个 project role. */
  @Transactional
  public ProjectMemberRoleAssignmentDetail addRoleToMember(Long projectMemberId, String role) {
    ProjectMember m =
        repo.findById(projectMemberId)
            .orElseThrow(() -> new NotFoundException("project member not found"));
    if (role == null || !ProjectMemberRole.ALL.contains(role)) {
      throw new BadRequestException("invalid role: " + role);
    }
    if (roleRepo.existsByProjectMemberIdAndProjectRole(m.getId(), role)) {
      throw new ConflictException("已挂该角色");
    }
    ProjectMemberRoleAssignment a = new ProjectMemberRoleAssignment();
    a.setProjectMemberId(m.getId());
    a.setProjectRole(role);
    return ProjectMemberRoleAssignmentDetail.from(roleRepo.saveAndFlush(a));
  }

  /** v0.0.88 (C8) — 移除一个 project role（不级联删 ProjectMember）. */
  @Transactional
  public void removeRoleFromMember(Long projectMemberId, String role) {
    if (!repo.existsById(projectMemberId)) {
      throw new NotFoundException("project member not found");
    }
    ProjectMemberRoleAssignment a =
        roleRepo
            .findByProjectMemberIdAndProjectRole(projectMemberId, role)
            .orElseThrow(() -> new NotFoundException("role assignment not found"));
    roleRepo.delete(a);
  }

  public List<ProjectMemberRoleAssignmentDetail> listRolesOfMember(Long projectMemberId) {
    if (!repo.existsById(projectMemberId)) {
      throw new NotFoundException("project member not found");
    }
    List<ProjectMemberRoleAssignment> all = roleRepo.findByProjectMemberId(projectMemberId);
    List<ProjectMemberRoleAssignmentDetail> out =
        new ArrayList<ProjectMemberRoleAssignmentDetail>(all.size());
    for (ProjectMemberRoleAssignment a : all) {
      out.add(ProjectMemberRoleAssignmentDetail.from(a));
    }
    return out;
  }

  /** v0.0.88 (C8) — 解析 projectMemberId → projectId（controller 鉴权用）. */
  public Long resolveProjectIdOfMember(Long projectMemberId) {
    ProjectMember m =
        repo.findById(projectMemberId)
            .orElseThrow(() -> new NotFoundException("project member not found"));
    return m.getProjectId();
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
    d.setRoles(Collections.singletonList(role));
    User u = users.get(userId);
    if (u != null) {
      d.setUserName(u.getName());
      d.setUserLoginName(u.getLoginName());
    }
    return d;
  }

  private ProjectMemberDetail enrich(
      ProjectMember m, Map<Long, User> usersOrNull, Map<Long, List<String>> rolesByMemberIdOrNull) {
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
    // v0.0.88 (C8) — roles 富化（合并 ProjectMember.role + assignment 表，sorted distinct）
    List<String> roles;
    if (rolesByMemberIdOrNull != null && rolesByMemberIdOrNull.containsKey(m.getId())) {
      roles = rolesByMemberIdOrNull.get(m.getId());
    } else {
      roles = loadRolesByMemberId(Collections.singletonList(m)).get(m.getId());
    }
    if (roles == null) roles = new ArrayList<String>();
    if (m.getRole() != null && !roles.contains(m.getRole())) {
      // include the legacy single role for back-compat
      List<String> merged = new ArrayList<String>(roles);
      merged.add(m.getRole());
      Collections.sort(merged);
      roles = merged;
    }
    d.setRoles(roles);
    return d;
  }

  private Map<Long, List<String>> loadRolesByMemberId(List<ProjectMember> members) {
    Map<Long, List<String>> out = new HashMap<Long, List<String>>();
    if (members == null || members.isEmpty()) return out;
    List<Long> ids = new ArrayList<Long>(members.size());
    for (ProjectMember m : members) ids.add(m.getId());
    List<ProjectMemberRoleAssignment> all = roleRepo.findByProjectMemberIdIn(ids);
    Map<Long, TreeSet<String>> tmp = new HashMap<Long, TreeSet<String>>();
    for (ProjectMemberRoleAssignment a : all) {
      TreeSet<String> s = tmp.get(a.getProjectMemberId());
      if (s == null) {
        s = new TreeSet<String>();
        tmp.put(a.getProjectMemberId(), s);
      }
      s.add(a.getProjectRole());
    }
    for (Long id : ids) {
      TreeSet<String> s = tmp.get(id);
      out.put(id, s == null ? new ArrayList<String>() : new ArrayList<String>(s));
    }
    return out;
  }

  private void persistRoleIfAbsent(Long projectMemberId, String role) {
    if (roleRepo.existsByProjectMemberIdAndProjectRole(projectMemberId, role)) return;
    ProjectMemberRoleAssignment a = new ProjectMemberRoleAssignment();
    a.setProjectMemberId(projectMemberId);
    a.setProjectRole(role);
    roleRepo.saveAndFlush(a);
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
