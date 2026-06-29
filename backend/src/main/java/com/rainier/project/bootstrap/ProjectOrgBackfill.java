/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.bootstrap;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.108 (H1) — Startup self-heal: backfill {@code rainier_project.organization_id} for legacy
 * rows left NULL.
 *
 * <p>Strategy per NULL row: take {@code ownerUserId} → primary {@code UserOrganization}
 * ({@code is_primary=1 AND left_at IS NULL}) → walk up {@code parentId} until first
 * {@link OrganizationType#DEPARTMENT} / {@link OrganizationType#DOMAIN} / {@link OrganizationType#COMPANY}
 * node (i.e. skip TEAM / SUBGROUP — the「非小组/团队」above the user's leaf membership). Write that
 * org id back.
 *
 * <p>Idempotent — subsequent boots only look at NULL rows; if walk fails (no primary org / orphan
 * parent / no qualifying ancestor) the row stays NULL and the next boot retries cheaply.
 *
 * <p>Flag-gated via {@code app.migration.project-org-backfill.enabled} (default true; test profile
 * flips false so legacy project tests keep direct control over the org column).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@ConditionalOnProperty(
    value = "app.migration.project-org-backfill.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ProjectOrgBackfill implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ProjectOrgBackfill.class);

  private final ProjectRepository projectRepo;
  private final UserOrganizationRepository userOrgRepo;
  private final OrganizationRepository organizationRepo;

  public ProjectOrgBackfill(
      ProjectRepository projectRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository organizationRepo) {
    this.projectRepo = projectRepo;
    this.userOrgRepo = userOrgRepo;
    this.organizationRepo = organizationRepo;
  }

  @Override
  @Transactional
  public void run(String... args) {
    int updated = 0;
    List<Project> all = projectRepo.findAll();
    for (Project p : all) {
      if (p.getOrganizationId() != null) {
        continue;
      }
      Long ownerId = p.getOwnerUserId();
      if (ownerId == null) {
        continue;
      }
      List<UserOrganization> primary =
          userOrgRepo.findByUserIdAndIsPrimaryTrueAndLeftAtIsNull(ownerId);
      if (primary.isEmpty()) {
        continue;
      }
      Long startOrgId = primary.get(0).getOrganizationId();
      Long resolved = resolveAncestor(startOrgId);
      if (resolved == null) {
        continue;
      }
      p.setOrganizationId(resolved);
      projectRepo.saveAndFlush(p);
      updated++;
    }
    if (updated > 0) {
      log.warn("ProjectOrgBackfill: backfilled {} rainier_project rows organization_id", updated);
    }
  }

  /**
   * Walk parentId upward from {@code startOrgId} until first DEPARTMENT/DOMAIN/COMPANY node.
   * Returns the id of that node, or null if no ancestor in the chain qualifies (e.g. orphan parent,
   * deleted row, or a chain that bottoms out below the wanted levels). Cycle-guarded via visited set.
   */
  private Long resolveAncestor(Long startOrgId) {
    Long cursor = startOrgId;
    Set<Long> visited = new HashSet<>();
    while (cursor != null && visited.add(cursor)) {
      Organization o = organizationRepo.findById(cursor).orElse(null);
      if (o == null) {
        return null;
      }
      OrganizationType t = o.getType();
      if (t == OrganizationType.DEPARTMENT
          || t == OrganizationType.DOMAIN
          || t == OrganizationType.COMPANY) {
        return o.getId();
      }
      cursor = o.getParentId();
    }
    return null;
  }
}
