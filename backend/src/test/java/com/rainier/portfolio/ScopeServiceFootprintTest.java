/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * v0.0.109 H2 — ScopeService.teamFootprintProjects: people-centric scope that walks the leader's
 * HEAD org-subtree, collects active members, returns every project any of them owns or has a
 * UserRole on. Orthogonal to {@code Project.organizationId} so legacy untagged projects surface.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScopeServiceFootprintTest {

  @Autowired private ScopeService scopeService;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;
  @Autowired private UserRoleRepository userRoleRepo;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    userOrgRepo.deleteAll();
    projectRepo.deleteAll();
    orgRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedOrg(String code, OrganizationType type, Long parentId) {
    Organization o = new Organization();
    o.setCode(code);
    o.setName(code);
    o.setType(type);
    o.setParentId(parentId);
    o.setEnabled(true);
    return orgRepo.saveAndFlush(o).getId();
  }

  private Long seedProject(String code, Long ownerId, Long orgId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(ownerId);
    p.setOrganizationId(orgId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private void memberOf(Long userId, Long orgId, UserOrgRole role, boolean active) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(orgId);
    uo.setRole(role);
    uo.setIsPrimary(false);
    uo.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
    if (!active) {
      uo.setLeftAt(Instant.parse("2026-02-01T00:00:00Z"));
    }
    userOrgRepo.saveAndFlush(uo);
  }

  private void roleOnProject(Long userId, Long projectId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(1L);
    ur.setProjectId(projectId);
    userRoleRepo.saveAndFlush(ur);
  }

  /** Scenario A — footprint walks subtree, collects owner + role projects, excludes outsiders. */
  @Test
  void teamFootprintProjects_includesSubtreeMembersOwnedAndRoledProjects() {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    Long charlie = seedUser("charlie");
    Long david = seedUser("david");
    Long dept = seedOrg("DEPT", OrganizationType.DEPARTMENT, null);
    Long team1 = seedOrg("TEAM1", OrganizationType.TEAM, dept);
    Long outside = seedOrg("OUTSIDE", OrganizationType.TEAM, null);
    memberOf(alice, dept, UserOrgRole.HEAD, true);
    memberOf(bob, team1, UserOrgRole.MEMBER, true);
    memberOf(charlie, team1, UserOrgRole.MEMBER, true);
    memberOf(david, outside, UserOrgRole.MEMBER, true);

    Long pBob = seedProject("P-BOB", bob, null); // untagged, surfaces via owner
    Long pCharlie = seedProject("P-CHARLIE", seedUser("ext"), null);
    roleOnProject(charlie, pCharlie);
    Long pBobDup = seedProject("P-BOB-DUP", bob, null); // bob both owns and has role
    roleOnProject(bob, pBobDup);
    seedProject("P-DAVID", david, null); // outside subtree → excluded

    List<Long> result = scopeService.teamFootprintProjects(alice);

    assertThat(result).containsExactlyInAnyOrder(pBob, pCharlie, pBobDup);
  }

  /** Scenario B — resolveProjectIds(scope=footprint) delegates to teamFootprintProjects. */
  @Test
  void resolveProjectIds_footprintDelegates() {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    memberOf(alice, team, UserOrgRole.HEAD, true);
    memberOf(bob, team, UserOrgRole.MEMBER, true);
    Long pBob = seedProject("P-BOB", bob, null);

    List<Long> result = scopeService.resolveProjectIds("alice", "footprint");

    assertThat(result).containsExactly(pBob);
  }

  /** Scenario C — non-HEAD user returns empty. */
  @Test
  void teamFootprintProjects_nonHeadReturnsEmpty() {
    Long eve = seedUser("eve");
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    memberOf(eve, team, UserOrgRole.MEMBER, true); // member, not HEAD

    assertThat(scopeService.teamFootprintProjects(eve)).isEmpty();
  }

  /** Scenario D — left-at members excluded. */
  @Test
  void teamFootprintProjects_excludesLeftMembers() {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    memberOf(alice, team, UserOrgRole.HEAD, true);
    memberOf(bob, team, UserOrgRole.MEMBER, false); // bob left
    seedProject("P-BOB", bob, null);

    assertThat(scopeService.teamFootprintProjects(alice)).isEmpty();
  }

  /** Scenario E (extra) — null leaderId guard. */
  @Test
  void teamFootprintProjects_nullLeaderReturnsEmpty() {
    assertThat(scopeService.teamFootprintProjects(null)).isEmpty();
  }
}
