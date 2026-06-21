/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
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

/**
 * v0.0.41 compliance dashboard — functional aggregation (admin-authz off in the default test profile,
 * so endpoints run unauthenticated here; gating is covered by {@link ComplianceAuthzTest}). Covers
 * TC-COMP-001..005.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ComplianceControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogRepository auditRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;

  @BeforeEach
  void cleanDb() {
    auditRepo.deleteAll();
    userRoleRepo.deleteAll();
    roleRepo.deleteAll();
    userRepo.deleteAll();
  }

  private void seedAudit(String entityType, String action) {
    AuditLog a = new AuditLog();
    a.setActor("system");
    a.setEntityType(entityType);
    a.setEntityId(1L);
    a.setAction(action);
    a.setSummary(action + " " + entityType);
    auditRepo.saveAndFlush(a);
  }

  private Long seedUser(String loginName, boolean enabled) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(enabled);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedRole(String code) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    return roleRepo.saveAndFlush(r).getId();
  }

  private void link(Long userId, Long roleId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(null);
    userRoleRepo.saveAndFlush(ur);
  }

  /** TC-COMP-001: audit-summary total + byAction (busiest first). */
  @Test
  void auditSummary_totalsAndByAction() throws Exception {
    seedAudit("REQUIREMENT", AuditAction.CREATE);
    seedAudit("REQUIREMENT", AuditAction.CREATE);
    seedAudit("TASK", AuditAction.CREATE);
    seedAudit("TASK", AuditAction.UPDATE);
    seedAudit("TASK", AuditAction.DELETE);

    mockMvc
        .perform(get("/api/compliance/audit-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(5))
        // CREATE (3) is busiest → first
        .andExpect(jsonPath("$.byAction[0].label").value("CREATE"))
        .andExpect(jsonPath("$.byAction[0].count").value(3))
        .andExpect(jsonPath("$.recent", hasSize(5)));
  }

  /** TC-COMP-002: audit-summary byEntityType. */
  @Test
  void auditSummary_byEntityType() throws Exception {
    seedAudit("TASK", AuditAction.CREATE);
    seedAudit("TASK", AuditAction.UPDATE);
    seedAudit("REQUIREMENT", AuditAction.CREATE);

    mockMvc
        .perform(get("/api/compliance/audit-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.byEntityType[0].label").value("TASK"))
        .andExpect(jsonPath("$.byEntityType[0].count").value(2));
  }

  /** TC-COMP-003: audit-summary empty → zeros, empty lists. */
  @Test
  void auditSummary_empty() throws Exception {
    mockMvc
        .perform(get("/api/compliance/audit-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.byAction", hasSize(0)))
        .andExpect(jsonPath("$.recent", hasSize(0)));
  }

  /** TC-COMP-004: residual-permissions = disabled users with role grants only. */
  @Test
  void residualPermissions_onlyDisabledWithRoles() throws Exception {
    Long ghost = seedUser("ghost", false); // disabled
    Long alice = seedUser("alice", true); // enabled
    seedUser("empty", false); // disabled, no roles
    Long role = seedRole("DEV");
    link(ghost, role); // residual!
    link(alice, role); // enabled → not residual

    mockMvc
        .perform(get("/api/compliance/residual-permissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].loginName").value("ghost"))
        .andExpect(jsonPath("$[0].roleCount").value(1))
        .andExpect(jsonPath("$[0].roleNames[0]").value("DEV"));
  }

  /** TC-COMP-005: no disabled-with-roles → empty list. */
  @Test
  void residualPermissions_none() throws Exception {
    Long alice = seedUser("alice", true);
    link(alice, seedRole("DEV"));

    mockMvc
        .perform(get("/api/compliance/residual-permissions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
