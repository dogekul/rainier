/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the set of project ids a caller can see under a given scope (v0.0.28) — the reusable
 * substrate behind every portfolio/dashboard. Scopes:
 *
 * <ul>
 *   <li>{@code mine} — projects the caller has a role on (UserRole.projectId) or owns.
 *   <li>{@code led} — projects tagged to an org the caller HEADs OR any of its descendant org nodes
 *       (department/domain → team subtree).
 *   <li>{@code all} — every project (portfolio-wide; PMO / exec).
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ScopeService {

  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final UserOrganizationRepository userOrgRepo;
  private final OrganizationRepository orgRepo;
  private final ProjectRepository projectRepo;

  public ScopeService(
      UserRepository userRepo,
      UserRoleRepository userRoleRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository orgRepo,
      ProjectRepository projectRepo) {
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.userOrgRepo = userOrgRepo;
    this.orgRepo = orgRepo;
    this.projectRepo = projectRepo;
  }

  /** Resolve project ids for {@code username} under {@code scope} (defaults to "mine"). */
  public List<Long> resolveProjectIds(String username, String scope) {
    String s = scope == null ? "mine" : scope.toLowerCase();
    if ("all".equals(s)) {
      return projectRepo.findAll().stream().map(Project::getId).collect(Collectors.toList());
    }
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return new ArrayList<>();
    }
    if ("led".equals(s)) {
      return ledProjectIds(me.getId());
    }
    return mineProjectIds(me.getId());
  }

  private List<Long> mineProjectIds(Long userId) {
    Set<Long> ids = new LinkedHashSet<>();
    for (UserRole ur : userRoleRepo.findByUserId(userId)) {
      if (ur.getProjectId() != null) {
        ids.add(ur.getProjectId());
      }
    }
    for (Project p : projectRepo.findByOwnerUserId(userId)) {
      ids.add(p.getId());
    }
    return new ArrayList<>(ids);
  }

  private List<Long> ledProjectIds(Long userId) {
    List<UserOrganization> heads =
        userOrgRepo.findByUserIdAndRoleAndLeftAtIsNull(userId, UserOrgRole.HEAD);
    Set<Long> rootOrgIds =
        heads.stream()
            .map(UserOrganization::getOrganizationId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (rootOrgIds.isEmpty()) {
      return new ArrayList<>();
    }
    Set<Long> orgIds = descendantOrgIds(rootOrgIds);
    return projectRepo.findByOrganizationIdIn(orgIds).stream()
        .map(Project::getId)
        .collect(Collectors.toList());
  }

  /** BFS the org tree (parentId) from the given roots, returning roots + all descendants. */
  Set<Long> descendantOrgIds(Set<Long> roots) {
    Set<Long> seen = new LinkedHashSet<>(roots);
    Deque<Long> queue = new ArrayDeque<>(roots);
    while (!queue.isEmpty()) {
      Long current = queue.poll();
      for (Organization child : orgRepo.findByParentId(current)) {
        if (seen.add(child.getId())) {
          queue.add(child.getId());
        }
      }
    }
    return seen;
  }
}
