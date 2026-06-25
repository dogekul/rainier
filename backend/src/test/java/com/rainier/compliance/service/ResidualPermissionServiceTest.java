/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.compliance.dto.RevokeResult;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.80 B7 — covers TC-RPR-001..007. Default test profile keeps admin-authz OFF so the MockMvc
 * paths run unauthenticated (gating is covered by {@code ComplianceAuthzTest}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ResidualPermissionServiceTest {

  @Autowired private ResidualPermissionService service;
  @Autowired private UserRepository userRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private AuditLogRepository auditRepo;
  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void clean() {
    auditRepo.deleteAll();
    userRoleRepo.deleteAll();
    userRoleRepo.flush();
    roleRepo.deleteAll();
    userRepo.deleteAll();
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

  /** TC-RPR-001: disabled + 2 roles → revoke clears both, writes 1 audit row. */
  @Test
  void revoke_disabledUserWithRoles_clearsAndAudits() {
    Long uid = seedUser("ghost", false);
    Long r1 = seedRole("DEV");
    Long r2 = seedRole("QA");
    link(uid, r1);
    link(uid, r2);

    RevokeResult res = service.revokeAllRoles(uid);

    assertTrue(res.isOk());
    assertEquals(2, res.getRevokedCount());
    assertTrue(userRoleRepo.findByUserId(uid).isEmpty());
    List<AuditLog> rows = auditRepo.findAll();
    assertEquals(1, rows.size());
    assertEquals(ResidualPermissionService.ACTION_REVOKE, rows.get(0).getAction());
    assertEquals("USER", rows.get(0).getEntityType());
    assertEquals(uid, rows.get(0).getEntityId());
  }

  /** TC-RPR-002: enabled user → revoke throws 400, role grant preserved. */
  @Test
  void revoke_enabledUser_throwsBadRequest() {
    Long uid = seedUser("alice", true);
    Long rid = seedRole("DEV");
    link(uid, rid);

    assertThrows(BadRequestException.class, () -> service.revokeAllRoles(uid));
    assertEquals(1, userRoleRepo.findByUserId(uid).size());
    assertEquals(0L, auditRepo.count());
  }

  /** TC-RPR-003: missing user → 404. */
  @Test
  void revoke_missingUser_throwsNotFound() {
    assertThrows(NotFoundException.class, () -> service.revokeAllRoles(9999L));
  }

  /** TC-RPR-004: disabled user with 0 roles → ok, count=0, no audit row written. */
  @Test
  void revoke_disabledUserNoRoles_noAuditWritten() {
    Long uid = seedUser("ghost", false);

    RevokeResult res = service.revokeAllRoles(uid);

    assertEquals(0, res.getRevokedCount());
    assertEquals(0L, auditRepo.count());
  }

  /** TC-RPR-005: disable-user on enabled+role → user disabled, role gone, 2 audit rows. */
  @Test
  void disableUser_enabledWithRole_disablesAndRevokes() {
    Long uid = seedUser("alice", true);
    Long rid = seedRole("DEV");
    link(uid, rid);

    RevokeResult res = service.disableAndRevoke(uid);

    assertTrue(res.isOk());
    assertEquals(1, res.getRevokedCount());
    assertFalse(res.isAlreadyDisabled());
    User reloaded = userRepo.findById(uid).get();
    assertFalse(reloaded.getEnabled());
    assertTrue(userRoleRepo.findByUserId(uid).isEmpty());
    assertEquals(2L, auditRepo.count());
  }

  /** TC-RPR-006: disable-user on already-disabled+role → alreadyDisabled=true, only REVOKE audit. */
  @Test
  void disableUser_alreadyDisabled_onlyRevokes() {
    Long uid = seedUser("ghost", false);
    Long rid = seedRole("DEV");
    link(uid, rid);

    RevokeResult res = service.disableAndRevoke(uid);

    assertTrue(res.isAlreadyDisabled());
    assertEquals(1, res.getRevokedCount());
    assertTrue(userRoleRepo.findByUserId(uid).isEmpty());
    List<AuditLog> rows = auditRepo.findAll();
    assertEquals(1, rows.size());
    assertEquals(ResidualPermissionService.ACTION_REVOKE, rows.get(0).getAction());
  }

  /** TC-RPR-007: disable-user on missing user → 404. */
  @Test
  void disableUser_missing_throwsNotFound() {
    assertThrows(NotFoundException.class, () -> service.disableAndRevoke(9999L));
  }

  /** HTTP surface — POST /api/compliance/users/{id}/revoke-roles returns ok JSON. */
  @Test
  void http_revokeRoles_returnsJson() throws Exception {
    Long uid = seedUser("ghost", false);
    Long rid = seedRole("DEV");
    link(uid, rid);

    mockMvc
        .perform(post("/api/compliance/users/" + uid + "/revoke-roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.revokedCount").value(1));
  }

  /** HTTP surface — POST /api/compliance/disable-user/{id} returns ok JSON. */
  @Test
  void http_disableUser_returnsJson() throws Exception {
    Long uid = seedUser("alice", true);
    Long rid = seedRole("DEV");
    link(uid, rid);

    mockMvc
        .perform(post("/api/compliance/disable-user/" + uid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.revokedCount").value(1))
        .andExpect(jsonPath("$.alreadyDisabled").value(false));
  }
}
