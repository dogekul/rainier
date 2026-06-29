/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.105 G1 — verifies {@link AdminRolePermissionController} write methods are now gated by
 * {@code @RequiresPermission(ROLE_MANAGE)} once {@code fine-grained-permissions} is on, and that
 * the bootstrap-seeded admin role passes the gate (no first-launch deadlock).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "app.security.admin-authz.enabled=true",
      "app.security.fine-grained-permissions.enabled=true"
    })
class AdminRolePermissionControllerAuthzTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private RolePermissionRepository rolePermRepo;

  private String aliceToken;
  private String bobToken;
  private Long targetRoleId;

  @BeforeEach
  void seed() {
    rolePermRepo.deleteAll();
    rolePermRepo.flush();
    userRoleRepo.deleteAll();
    userRoleRepo.flush();
    roleRepo.deleteAll();
    userRepo.deleteAll();

    Long aliceId = seedUser("alice-g1");
    Long bobId = seedUser("bob-g1");

    Long fullAdmin = seedAdminRole("FULL-ADM");
    Long plainAdmin = seedAdminRole("PLAIN-ADM");
    targetRoleId = seedAdminRole("TARGET");

    // alice has ROLE_MANAGE — represents the bootstrap-seeded state.
    grant(fullAdmin, PermissionPoint.ROLE_MANAGE);
    // bob is admin but ROLE_MANAGE was revoked (or never bound) — must be blocked.

    link(aliceId, fullAdmin);
    link(bobId, plainAdmin);

    aliceToken = authService.issueToken("alice-g1");
    bobToken = authService.issueToken("bob-g1");
  }

  /** Scenario 4: admin with ROLE_MANAGE -> 200. */
  @Test
  void grant_withRoleManage_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/roles/" + targetRoleId + "/permissions")
                .header("Authorization", "Bearer " + aliceToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionPoint\":\"AUDIT_VIEW\"}"))
        .andExpect(status().isOk());
  }

  /** Scenario 5: admin WITHOUT ROLE_MANAGE -> 403. */
  @Test
  void grant_withoutRoleManage_returns403() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/roles/" + targetRoleId + "/permissions")
                .header("Authorization", "Bearer " + bobToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"permissionPoint\":\"AUDIT_VIEW\"}"))
        .andExpect(status().isForbidden());
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
