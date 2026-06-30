/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.service;

import com.rainier.auth.dto.MeResponse;
import com.rainier.auth.dto.MeResponse.MeProject;
import com.rainier.auth.dto.MeResponse.MeRole;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectadmin.service.ProjectAdminService;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles the {@code GET /api/auth/me} current-user context from the token subject (loginName).
 *
 * <p>Resolves the User, then batch-enriches its UserRole rows with Role + Project (no N+1). Degrades
 * gracefully when the subject has no matching User (mock "system"): id null, empty roles/projects.
 */
@Service
@Transactional(readOnly = true)
public class MeService {

  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final RoleRepository roleRepo;
  private final ProjectRepository projectRepo;
  private final ProjectMemberRepository projectMemberRepo;
  private final ProjectAdminService projectAdminService;
  private final RequirementRepository requirementRepo;

  public MeService(
      UserRepository userRepo,
      UserRoleRepository userRoleRepo,
      RoleRepository roleRepo,
      ProjectRepository projectRepo,
      ProjectMemberRepository projectMemberRepo,
      ProjectAdminService projectAdminService,
      RequirementRepository requirementRepo) {
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.roleRepo = roleRepo;
    this.projectRepo = projectRepo;
    this.projectMemberRepo = projectMemberRepo;
    this.projectAdminService = projectAdminService;
    this.requirementRepo = requirementRepo;
  }

  public MeResponse forUsername(String username) {
    User user = userRepo.findByLoginName(username).orElse(null);
    if (user == null) {
      // Valid token but no matching User (e.g. the mock "system" auditor) — degrade, don't 500.
      return new MeResponse(
          null,
          username,
          null,
          Collections.<MeRole>emptyList(),
          Collections.<MeProject>emptyList(),
          "BASIC",
          Collections.<Long>emptyList(),
          "/");
    }
    List<UserRole> assignments = userRoleRepo.findByUserId(user.getId());

    Set<Long> roleIds =
        assignments.stream()
            .map(UserRole::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Set<Long> projectIds =
        assignments.stream()
            .map(UserRole::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    Map<Long, Role> roleMap = new LinkedHashMap<>();
    roleRepo.findAllById(roleIds).forEach(r -> roleMap.put(r.getId(), r));
    Map<Long, Project> projectMap = new LinkedHashMap<>();
    projectRepo.findAllById(projectIds).forEach(p -> projectMap.put(p.getId(), p));

    List<MeRole> roles = new ArrayList<>();
    for (UserRole ur : assignments) {
      Role r = roleMap.get(ur.getRoleId());
      Project p = ur.getProjectId() == null ? null : projectMap.get(ur.getProjectId());
      roles.add(
          new MeRole(
              ur.getRoleId(),
              r == null ? null : r.getCode(),
              r == null ? null : r.getName(),
              ur.getProjectId(),
              p == null ? null : p.getName(),
              p == null ? null : p.getCode(),
              // Never null: getAdminAccess() already coalesces NULL→false; missing role → false.
              r == null ? Boolean.FALSE : r.getAdminAccess()));
    }

    // Distinct projects (first-seen order); org-level roles (projectId null) add no project.
    Map<Long, MeProject> projects = new LinkedHashMap<>();
    for (UserRole ur : assignments) {
      Long pid = ur.getProjectId();
      if (pid != null && !projects.containsKey(pid)) {
        Project p = projectMap.get(pid);
        if (p != null) {
          projects.put(pid, new MeProject(p.getId(), p.getCode(), p.getName()));
        }
      }
    }

    // v0.0.64 — also include projects where the user is a registered project_member (regardless of user_role).
    // Owner-of-project membership is implicit (owner can manage own projects through 我负责的 list elsewhere);
    // here we focus on the user_role+project_member UNION so 「我的项目」 covers all involvement.
    List<ProjectMember> memberships = projectMemberRepo.findByUserId(user.getId());
    if (!memberships.isEmpty()) {
      Set<Long> additionalProjectIds =
          memberships.stream()
              .map(ProjectMember::getProjectId)
              .filter(pid -> !projects.containsKey(pid))
              .collect(Collectors.toSet());
      if (!additionalProjectIds.isEmpty()) {
        projectRepo
            .findAllById(additionalProjectIds)
            .forEach(p -> projects.put(p.getId(), new MeProject(p.getId(), p.getCode(), p.getName())));
      }
    }

    List<Long> adminProjectIds = projectAdminService.listAdminProjects(user.getId());

    return new MeResponse(
        user.getId(),
        username,
        user.getName(),
        roles,
        new ArrayList<>(projects.values()),
        user.getAiAuthLevel(),
        adminProjectIds,
        defaultLandingPath(user, roles));
  }

  private String defaultLandingPath(User user, List<MeRole> roles) {
    for (MeRole r : roles) {
      if (Boolean.TRUE.equals(r.getAdminAccess())) {
        return "/sys/compliance";
      }
    }
    for (MeRole r : roles) {
      if ("PMO".equalsIgnoreCase(r.getRoleCode())) {
        return "/pmo";
      }
    }
    for (MeRole r : roles) {
      if ("ARCHITECT".equalsIgnoreCase(r.getRoleCode())) {
        return "/architect";
      }
    }
    if (projectRepo.existsByOwnerUserId(user.getId())) {
      return "/pm/cockpit";
    }
    if (requirementRepo.existsByOwnerUserId(user.getId())) {
      return "/inbox";
    }
    return "/";
  }
}
