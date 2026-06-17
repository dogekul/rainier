/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.21: admin-endpoint authorization with the gate flipped ON (the default test profile keeps it
 * off so legacy controller tests are unaffected). Covers TC-AUTHZ-001..011.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.admin-authz.enabled=true")
class AdminAuthorizationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private ObjectMapper json;

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

  /** TC-AUTHZ-001: 无 token Tier A 写 → 401. */
  @Test
  void noToken_tierAWrite_returns401() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "X1");
    body.put("name", "X1");
    mockMvc
        .perform(post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isUnauthorized());
  }

  /** TC-AUTHZ-002: 无 token Tier A 读 → 401. */
  @Test
  void noToken_tierARead_returns401() throws Exception {
    mockMvc.perform(get("/api/audit-logs")).andExpect(status().isUnauthorized());
  }

  /** TC-AUTHZ-003: 非管理员 Tier A 写 → 403. */
  @Test
  void nonAdmin_tierAWrite_returns403() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "X2");
    body.put("name", "X2");
    mockMvc
        .perform(
            post("/api/roles")
                .header("Authorization", bearer(nonAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isForbidden());
  }

  /** TC-AUTHZ-004: 非管理员 Tier A 删 → 403. */
  @Test
  void nonAdmin_tierADelete_returns403() throws Exception {
    mockMvc
        .perform(delete("/api/organizations/1").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isForbidden());
  }

  /** TC-AUTHZ-005: 管理员 Tier A 写 → 放行（2xx，非 401/403）. */
  @Test
  void admin_tierAWrite_passes() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "NEWROLE");
    body.put("name", "新角色");
    mockMvc
        .perform(
            post("/api/roles")
                .header("Authorization", bearer(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  /** TC-AUTHZ-006: 非管理员 Tier B 读 (users) → 200. */
  @Test
  void nonAdmin_tierBReadUsers_returns200() throws Exception {
    mockMvc
        .perform(get("/api/users").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isOk());
  }

  /** TC-AUTHZ-007: 非管理员 Tier B 读 (features / product-modules) → 200. */
  @Test
  void nonAdmin_tierBReadFeaturesAndModules_returns200() throws Exception {
    mockMvc
        .perform(get("/api/features").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/product-modules").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isOk());
  }

  /** TC-AUTHZ-008: 非管理员 Tier B 写 (users) → 403. */
  @Test
  void nonAdmin_tierBWriteUsers_returns403() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "charlie");
    body.put("name", "Charlie");
    mockMvc
        .perform(
            post("/api/users")
                .header("Authorization", bearer(nonAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isForbidden());
  }

  /** TC-AUTHZ-009: 全员端点不被误伤 — 非管理员 GET /api/projects → 200. */
  @Test
  void nonAdmin_allUsersEndpoint_returns200() throws Exception {
    mockMvc
        .perform(get("/api/projects").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isOk());
  }

  /** TC-AUTHZ-010: OPTIONS 预检真实放行（200，非 401/403）. */
  @Test
  void optionsPreflight_notBlocked() throws Exception {
    mockMvc.perform(options("/api/roles")).andExpect(status().isOk());
  }

  /** TC-AUTHZ-011: 无效 token Tier A 读 → 401. */
  @Test
  void invalidToken_tierARead_returns401() throws Exception {
    mockMvc
        .perform(get("/api/roles").header("Authorization", "Bearer not-a-real-jwt"))
        .andExpect(status().isUnauthorized());
  }

  /**
   * TC-AUTHZ-012: sibling-prefix 边界 — `/api/products`(Tier A) 读收口 vs `/api/product-modules`(Tier B)
   * 读放行，证明 AdminPaths.matches 不把 products 误当 product-modules（反之亦然）.
   */
  @Test
  void siblingPrefix_productsGatedButProductModulesOpen() throws Exception {
    mockMvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/product-modules").header("Authorization", bearer(nonAdminToken)))
        .andExpect(status().isOk());
  }

  /** TC-AUTHZ-013: 非管理员 Tier B 写 (features) → 403（不仅 users）. */
  @Test
  void nonAdmin_tierBWriteFeatures_returns403() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("name", "f");
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/features/1")
                .header("Authorization", bearer(nonAdminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isForbidden());
  }

  /**
   * TC-AUTHZ-014 (PA-1 回归): matrix-param 不能绕过鉴权 — `/api/roles;x=1` 无 token 仍 401（拦截器按
   * Spring lookup path 匹配，剥掉 `;...`，与 DispatcherServlet 路由一致）.
   */
  @Test
  void matrixParam_doesNotBypassGate() throws Exception {
    mockMvc.perform(get("/api/roles;x=1")).andExpect(status().isUnauthorized());
  }
}
