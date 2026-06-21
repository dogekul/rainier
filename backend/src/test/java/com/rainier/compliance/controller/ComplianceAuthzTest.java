/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.41 — /api/compliance/** is admin-gated (Tier A). Gate flipped ON via @TestPropertySource (the
 * default test profile keeps it off). Covers TC-COMP-AUTHZ-001..005.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.admin-authz.enabled=true")
class ComplianceAuthzTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;

  private String adminToken;
  private String nonAdminToken;

  @BeforeEach
  void seed() {
    userRoleRepo.deleteAll();
    userRoleRepo.flush();
    roleRepo.deleteAll();
    userRepo.deleteAll();
    Long aliceId = seedUser("alice");
    Long bobId = seedUser("bob");
    Long adminRole = seedRole("ADMIN", true);
    Long plainRole = seedRole("DEV", false);
    link(aliceId, adminRole);
    link(bobId, plainRole);
    adminToken = authService.issueToken("alice");
    nonAdminToken = authService.issueToken("bob");
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

  private static String bearer(String token) {
    return "Bearer " + token;
  }

  /** TC-COMP-AUTHZ-001: no token → 401 (audit-summary). */
  @Test
  void noToken_auditSummary_returns401() throws Exception {
    mockMvc.perform(get("/api/compliance/audit-summary")).andExpect(status().isUnauthorized());
  }

  /** TC-COMP-AUTHZ-006: no token → 401 (residual-permissions — same Tier A gate). */
  @Test
  void noToken_residual_returns401() throws Exception {
    mockMvc
        .perform(get("/api/compliance/residual-permissions"))
        .andExpect(status().isUnauthorized());
  }

  /** TC-COMP-AUTHZ-002: non-admin → 403 (audit-summary). */
  @Test
  void nonAdmin_auditSummary_returns403() throws Exception {
    mockMvc
        .perform(get("/api/compliance/audit-summary").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isForbidden());
  }

  /** TC-COMP-AUTHZ-003: admin → 200 (audit-summary). */
  @Test
  void admin_auditSummary_returns200() throws Exception {
    mockMvc
        .perform(get("/api/compliance/audit-summary").header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk());
  }

  /** TC-COMP-AUTHZ-004: non-admin → 403 (residual-permissions). */
  @Test
  void nonAdmin_residual_returns403() throws Exception {
    mockMvc
        .perform(
            get("/api/compliance/residual-permissions")
                .header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isForbidden());
  }

  /** TC-COMP-AUTHZ-005: admin → 200 (residual-permissions). */
  @Test
  void admin_residual_returns200() throws Exception {
    mockMvc
        .perform(
            get("/api/compliance/residual-permissions").header("Authorization", bearer(adminToken)))
        .andExpect(status().isOk());
  }
}
