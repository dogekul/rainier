/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.authz.PermissionPoint;
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
 * v0.0.77 B4 — fine-grained @RequiresPermission end-to-end via MockMvc.
 *
 * <p>Both gates flipped ON (admin-authz + fine-grained-permissions). alice = admin + AUDIT_VIEW;
 * bob = admin BUT no AUDIT_VIEW (regression: passing the admin gate must NOT auto-grant points).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "app.security.admin-authz.enabled=true",
      "app.security.fine-grained-permissions.enabled=true"
    })
class PermissionInterceptorTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private RolePermissionRepository rolePermRepo;

  private String aliceToken;
  private String bobToken;

  @BeforeEach
  void seed() {
    rolePermRepo.deleteAll();
    userRoleRepo.deleteAll();
    userRoleRepo.flush();
    roleRepo.deleteAll();
    userRepo.deleteAll();

    Long aliceId = seedUser("alice-pi");
    Long bobId = seedUser("bob-pi");

    // Both roles get adminAccess=true so they clear the admin gate; the differentiator is the
    // permission-point grant.
    Long auditorAdmin = seedAdminRole("AUDITOR-ADM");
    Long plainAdmin = seedAdminRole("PLAIN-ADM");
    grant(auditorAdmin, PermissionPoint.AUDIT_VIEW);

    link(aliceId, auditorAdmin);
    link(bobId, plainAdmin);

    aliceToken = authService.issueToken("alice-pi");
    bobToken = authService.issueToken("bob-pi");
  }

  /** 已声明 @RequiresPermission(AUDIT_VIEW) + 用户有权限 → 200. */
  @Test
  void annotatedEndpoint_withPermission_returns200() throws Exception {
    mockMvc
        .perform(get("/api/audit-logs").header("Authorization", "Bearer " + aliceToken))
        .andExpect(status().isOk());
  }

  /** 已声明 + admin身份但没有权限点 → 403（关键：admin 不再是免死金牌）. */
  @Test
  void annotatedEndpoint_adminWithoutPermission_returns403() throws Exception {
    mockMvc
        .perform(get("/api/audit-logs").header("Authorization", "Bearer " + bobToken))
        .andExpect(status().isForbidden());
  }

  /** 已声明 + 无 token → 401（admin gate 先拦下；语义一致即可）. */
  @Test
  void annotatedEndpoint_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/audit-logs")).andExpect(status().isUnauthorized());
  }

  /** 未声明的 admin 端点（/api/roles GET）— 走 AdminPaths 兜底，admin 通过. */
  @Test
  void unannotatedAdminEndpoint_adminWithoutFineGrain_stillPasses() throws Exception {
    mockMvc
        .perform(get("/api/roles").header("Authorization", "Bearer " + bobToken))
        .andExpect(status().isOk());
  }

  /** Compliance 端点用 ANY 多点：用户拥有 AUDIT_VIEW 也应放行（验证 level=ANY 默认语义）. */
  @Test
  void annotatedEndpoint_anyOfMultiplePoints_passesWithEither() throws Exception {
    mockMvc
        .perform(get("/api/compliance/audit-summary").header("Authorization", "Bearer " + aliceToken))
        .andExpect(status().isOk());
  }

  private Long seedUser(String login) {
    User u = new User();
    u.setLoginName(login);
    u.setName(login);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedAdminRole(String code) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    r.setAdminAccess(true);
    return roleRepo.saveAndFlush(r).getId();
  }

  private void grant(Long roleId, PermissionPoint pp) {
    RolePermission rp = new RolePermission();
    rp.setRoleId(roleId);
    rp.setPermissionPoint(pp.name());
    rolePermRepo.saveAndFlush(rp);
  }

  private void link(Long userId, Long roleId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(null);
    userRoleRepo.saveAndFlush(ur);
  }
}
