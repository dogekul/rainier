/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** H6 — GET /api/auth/me returns a deterministic role-based default landing path. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeDefaultLandingTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private RequirementRepository requirementRepo;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    roleRepo.deleteAll();
    userRepo.deleteAll();
  }

  /** H6-S1: admin users land on the compliance dashboard. */
  @Test
  void me_adminRole_returnsComplianceDefaultLanding() throws Exception {
    Long uid = seedUser("alice");
    Long roleId = seedRole("ADMIN", true);
    link(uid, roleId);

    assertLanding("alice", "/sys/compliance");
  }

  /** H6-S2: PMO beats project ownership for non-admin users. */
  @Test
  void me_pmoRole_returnsPmoDefaultLanding() throws Exception {
    Long uid = seedUser("pmo");
    Long roleId = seedRole("PMO", false);
    link(uid, roleId);
    seedProject("PRJ-PMO", uid);

    assertLanding("pmo", "/pmo");
  }

  /** H6-S3: ARCHITECT role lands on the architect workbench. */
  @Test
  void me_architectRole_returnsArchitectDefaultLanding() throws Exception {
    Long uid = seedUser("arch");
    Long roleId = seedRole("ARCHITECT", false);
    link(uid, roleId);

    assertLanding("arch", "/architect");
  }

  /** H6-S4: project owners without stronger roles land on PM cockpit. */
  @Test
  void me_projectOwner_returnsCockpitDefaultLanding() throws Exception {
    Long uid = seedUser("pm");
    seedProject("PRJ-PM", uid);

    assertLanding("pm", "/pm/cockpit");
  }

  /** H6-S5: requirement owners without project ownership land on PO inbox. */
  @Test
  void me_requirementOwner_returnsInboxDefaultLanding() throws Exception {
    Long uid = seedUser("po");
    seedRequirement("REQ-PO", uid);

    assertLanding("po", "/inbox");
  }

  /** H6-S6: users without a special role or ownership land on the workbench. */
  @Test
  void me_plainUser_returnsHomeDefaultLanding() throws Exception {
    seedUser("dev");

    assertLanding("dev", "/");
  }

  private void assertLanding(String loginName, String expected) throws Exception {
    String token = authService.issueToken(loginName);
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.defaultLandingPath").value(expected));
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedRole(String code, boolean adminAccess) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    r.setAdminAccess(adminAccess);
    return roleRepo.saveAndFlush(r).getId();
  }

  private void link(Long userId, Long roleId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(null);
    userRoleRepo.saveAndFlush(ur);
  }

  private void seedProject(String code, Long ownerId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("PLANNING");
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    projectRepo.saveAndFlush(p);
  }

  private void seedRequirement(String code, Long ownerId) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle(code);
    r.setOwnerUserId(ownerId);
    r.setStatus("OPEN");
    r.setPriority("P1");
    requirementRepo.saveAndFlush(r);
  }
}
