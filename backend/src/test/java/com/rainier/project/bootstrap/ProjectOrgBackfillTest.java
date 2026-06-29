/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * v0.0.108 (H1) — TC-PRJ-ORG-003..006: ProjectOrgBackfill walk-up + idempotency + non-NULL skip
 * + missing-primary-org safe skip.
 *
 * <p>Mirrors {@link ProjectTypeBackfillTest}: non-transactional so {@code saveAndFlush} commits,
 * the runner's own {@code @Transactional} commits, and re-reads see fresh data. Flag flipped on
 * via {@code @TestPropertySource} because the default test profile has it OFF (parity with other
 * backfill runners).
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.migration.project-org-backfill.enabled=true")
class ProjectOrgBackfillTest {

  @Autowired private ProjectOrgBackfill backfill;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;

  @BeforeEach
  void cleanDb() {
    projectRepo.deleteAll();
    userOrgRepo.deleteAll();
    userRepo.deleteAll();
    orgRepo.deleteAll();
  }

  private Long createUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Organization createOrg(String code, OrganizationType type, Long parentId) {
    Organization o = new Organization();
    o.setCode(code);
    o.setName(code);
    o.setType(type);
    o.setParentId(parentId);
    o.setEnabled(true);
    return orgRepo.saveAndFlush(o);
  }

  private void assignPrimary(Long userId, Long orgId) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(orgId);
    uo.setRole(UserOrgRole.MEMBER);
    uo.setIsPrimary(true);
    uo.setJoinedAt(Instant.now());
    userOrgRepo.saveAndFlush(uo);
  }

  private Long createProjectWithNullOrg(Long ownerId, String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("PLANNING");
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    p.setOrganizationId(null);
    return projectRepo.saveAndFlush(p).getId();
  }

  /** TC-PRJ-ORG-003: SUBGROUP → TEAM → DEPARTMENT 链上溯到 DEPARTMENT 节点写回. */
  @Test
  void run_walksUpToDepartment() {
    Long aliceId = createUser("alice");
    Organization dept = createOrg("DEPT", OrganizationType.DEPARTMENT, null);
    Organization team = createOrg("TEAM", OrganizationType.TEAM, dept.getId());
    Organization sg = createOrg("SG", OrganizationType.SUBGROUP, team.getId());
    assignPrimary(aliceId, sg.getId());
    Long pid = createProjectWithNullOrg(aliceId, "P-WALK");

    backfill.run();

    Project reloaded = projectRepo.findById(pid).orElseThrow(IllegalStateException::new);
    assertEquals(dept.getId(), reloaded.getOrganizationId());
  }

  /** TC-PRJ-ORG-004: 幂等 — 再跑一次不改已写值. */
  @Test
  void run_isIdempotent() {
    Long aliceId = createUser("alice");
    Organization dept = createOrg("DEPT", OrganizationType.DEPARTMENT, null);
    assignPrimary(aliceId, dept.getId());
    Long pid = createProjectWithNullOrg(aliceId, "P-IDEMP");

    backfill.run();
    Long firstWrite =
        projectRepo.findById(pid).orElseThrow(IllegalStateException::new).getOrganizationId();
    backfill.run();
    Long secondRead =
        projectRepo.findById(pid).orElseThrow(IllegalStateException::new).getOrganizationId();

    assertEquals(dept.getId(), firstWrite);
    assertEquals(firstWrite, secondRead);
  }

  /** TC-PRJ-ORG-005: 非 NULL 行不被回填. */
  @Test
  void run_skipsNonNullRows() {
    Long aliceId = createUser("alice");
    Organization dept = createOrg("DEPT", OrganizationType.DEPARTMENT, null);
    assignPrimary(aliceId, dept.getId());
    Project p = new Project();
    p.setCode("P-PRESET");
    p.setName("P-PRESET");
    p.setStatus("PLANNING");
    p.setOwnerUserId(aliceId);
    p.setEnabled(true);
    p.setOrganizationId(999L); // 显式非空
    Long pid = projectRepo.saveAndFlush(p).getId();

    backfill.run();

    assertEquals(
        999L,
        projectRepo.findById(pid).orElseThrow(IllegalStateException::new).getOrganizationId(),
        "已写入的 organization_id 必须保持不变");
  }

  /** TC-PRJ-ORG-006: owner 无主组织 → 安全留空（不抛）. */
  @Test
  void run_ownerHasNoPrimaryOrg_leavesNull() {
    Long bobId = createUser("bob");
    Long pid = createProjectWithNullOrg(bobId, "P-NOORG");

    backfill.run();

    assertNull(
        projectRepo.findById(pid).orElseThrow(IllegalStateException::new).getOrganizationId());
  }
}
