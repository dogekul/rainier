/* (C) 2026 Rainier — internal use only. */
package com.rainier.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.organizationpmo.domain.OrganizationPmo;
import com.rainier.organizationpmo.repository.OrganizationPmoRepository;
import com.rainier.project.dto.ProjectCreateRequest;
import com.rainier.project.dto.ProjectDetail;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.project.service.ProjectService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.64 — Project default-value injection (team / pmo). Covers TC-PROJ-MOD-001~005. */
@SpringBootTest
@ActiveProfiles("test")
class ProjectServiceDefaultInjectionTest {

  @Autowired private ProjectService projectService;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;
  @Autowired private OrganizationPmoRepository orgPmoRepo;
  @Autowired private UserRepository userRepo;

  private Long ownerId;
  private Long orgId;
  private Long pmoUserId;

  @BeforeEach
  void seed() {
    projectRepo.deleteAll();
    orgPmoRepo.deleteAll();
    userOrgRepo.deleteAll();
    orgRepo.deleteAll();
    userRepo.deleteAll();

    User owner = new User();
    owner.setLoginName("lina");
    owner.setName("李娜");
    owner.setEnabled(Boolean.TRUE);
    ownerId = userRepo.saveAndFlush(owner).getId();

    User pmo = new User();
    pmo.setLoginName("liling");
    pmo.setName("黎立");
    pmo.setEnabled(Boolean.TRUE);
    pmoUserId = userRepo.saveAndFlush(pmo).getId();

    Organization o = new Organization();
    o.setCode("ORG-A");
    o.setName("研发中心");
    o.setType(OrganizationType.DEPARTMENT);
    o.setEnabled(Boolean.TRUE);
    orgId = orgRepo.saveAndFlush(o).getId();
  }

  private void joinOrgPrimary(Long userId, Long organizationId) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(organizationId);
    uo.setRole(UserOrgRole.MEMBER);
    uo.setIsPrimary(Boolean.TRUE);
    uo.setJoinedAt(Instant.now());
    userOrgRepo.saveAndFlush(uo);
  }

  private void seedOrgPmo(Long organizationId, Long userId) {
    OrganizationPmo p = new OrganizationPmo();
    p.setOrganizationId(organizationId);
    p.setUserId(userId);
    orgPmoRepo.saveAndFlush(p);
  }

  private ProjectCreateRequest baseReq() {
    ProjectCreateRequest req = new ProjectCreateRequest();
    req.setName("X");
    req.setOwnerUserId(ownerId);
    req.setProjectType("CASUAL");
    return req;
  }

  /** TC-PROJ-MOD-001 create 带 pmoUserId → enriched pmoName 回返. */
  @Test
  void create_with_pmo_explicit() {
    ProjectCreateRequest req = baseReq();
    req.setOrganizationId(orgId);
    req.setPmoUserId(pmoUserId);
    ProjectDetail d = projectService.create(req);
    assertEquals(orgId, d.getOrganizationId());
    assertEquals(pmoUserId, d.getPmoUserId());
    assertEquals("黎立", d.getPmoName());
    assertEquals("研发中心", d.getOrganizationName());
  }

  /** TC-PROJ-MOD-002 create 带 organizationId → enriched organizationName 回返. */
  @Test
  void create_with_org_explicit_returns_enriched() {
    ProjectCreateRequest req = baseReq();
    req.setOrganizationId(orgId);
    ProjectDetail d = projectService.create(req);
    assertEquals(orgId, d.getOrganizationId());
    assertEquals("研发中心", d.getOrganizationName());
  }

  /** TC-PROJ-MOD-003 缺 organizationId + owner 有主组织 → 自动填. */
  @Test
  void missing_org_auto_injects_owner_primary_org() {
    joinOrgPrimary(ownerId, orgId);
    ProjectCreateRequest req = baseReq();  // 不传 organizationId
    ProjectDetail d = projectService.create(req);
    assertEquals(orgId, d.getOrganizationId());
  }

  /** TC-PROJ-MOD-004 缺 pmoUserId 但有 team → 取 team effective-PMOs 首条. */
  @Test
  void missing_pmo_auto_injects_team_first_pmo() {
    joinOrgPrimary(ownerId, orgId);
    seedOrgPmo(orgId, pmoUserId);
    ProjectCreateRequest req = baseReq();  // 不传 organizationId / pmoUserId
    ProjectDetail d = projectService.create(req);
    assertEquals(orgId, d.getOrganizationId());
    assertEquals(pmoUserId, d.getPmoUserId());
  }

  /** TC-PROJ-MOD-005 owner 无主组织 → organizationId / pmoUserId 都 null. */
  @Test
  void owner_without_primary_org_keeps_org_null() {
    ProjectCreateRequest req = baseReq();  // owner 无 user_organization 行
    ProjectDetail d = projectService.create(req);
    assertNotNull(d.getId());
    assertNull(d.getOrganizationId());
    assertNull(d.getPmoUserId());
  }
}
