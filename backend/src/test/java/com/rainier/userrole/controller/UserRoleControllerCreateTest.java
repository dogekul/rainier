/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for {@link UserRoleController} POST + 占位语义. Covers TC-UROL-001..007. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRoleControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createRole() {
    Role r = new Role();
    r.setCode("PMO");
    r.setName("PMO");
    r.setEnabled(true);
    return roleRepo.saveAndFlush(r).getId();
  }

  /** TC-UROL-001: 含 projectId 合法关联创建。 */
  @Test
  void post_withProjectId_returns201() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", 42L);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.roleId").value(roleId))
        .andExpect(jsonPath("$.projectId").value(42));
  }

  /** TC-UROL-002: projectId=null 公司级 hat 创建。 */
  @Test
  void post_withNullProjectId_returns201() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.putNull("projectId");
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").isEmpty());
  }

  /** TC-UROL-003: (userId, roleId, projectId=42) 重复 → 409 (DB UNIQUE 路径). */
  @Test
  void post_duplicateNonNullProjectId_returns409() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", 42L);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("user-role already exists")));
  }

  /** TC-UROL-004: (userId, roleId, projectId=null) 重复 → 409 (service NULL 兜底). */
  @Test
  void post_duplicateNullProjectId_returns409() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.putNull("projectId");
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("user-role already exists")));
  }

  /** TC-UROL-005: NULL 与 non-NULL 共存。 */
  @Test
  void post_nullAndNonNullProjectId_coexistOk() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body1 = json.createObjectNode();
    body1.put("userId", userId);
    body1.put("roleId", roleId);
    body1.putNull("projectId");
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body1.toString()))
        .andExpect(status().isCreated());

    ObjectNode body2 = json.createObjectNode();
    body2.put("userId", userId);
    body2.put("roleId", roleId);
    body2.put("projectId", 42L);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body2.toString()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/user-roles?userId=" + userId + "&roleId=" + roleId))
        .andExpect(jsonPath("$.total").value(2));
  }

  /** TC-UROL-006: userId 不存在 → 400. */
  @Test
  void post_unknownUserId_returns400() throws Exception {
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", 999_999L);
    body.put("roleId", roleId);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("user not found")));
  }

  /** TC-UROL-007: projectId 任意 BIGINT 不校验（占位语义）。 */
  @Test
  void post_arbitraryProjectId_accepted() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", 987_654_321L);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(987_654_321L));
  }
}
