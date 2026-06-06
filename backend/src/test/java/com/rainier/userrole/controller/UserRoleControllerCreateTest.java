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
    if (projectRepo != null) {
      projectRepo.deleteAll();
    }
    userRepo.deleteAll();
    roleRepo.deleteAll();
  }

  @Autowired(required = false)
  private com.rainier.project.repository.ProjectRepository projectRepo;

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

  /** TC-UROL-001: 含 projectId 合法关联创建。 v0.0.8: projectId 现在校验存在 — seed real Project first. */
  @Test
  void post_withProjectId_returns201() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    Long projectId = seedProject(userId, "PROJ-UR-V07-001");
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.roleId").value(roleId))
        .andExpect(jsonPath("$.projectId").value(projectId));
  }

  private Long seedProject(Long ownerId, String code) {
    com.rainier.project.domain.Project p = new com.rainier.project.domain.Project();
    p.setCode(code);
    p.setName("seed");
    p.setStatus(com.rainier.project.domain.ProjectStatus.PLANNING);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
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

  /**
   * TC-UROL-003: (userId, roleId, projectId=X) 重复 → 409 (DB UNIQUE 路径). v0.0.8: seed real Project.
   */
  @Test
  void post_duplicateNonNullProjectId_returns409() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    Long projectId = seedProject(userId, "PROJ-UR-V07-003");
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", projectId);
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
    // v0.0.8: projectId now validated — seed Project before reference.
    body2.put("projectId", seedProject(userId, "PROJ-UR-V07-005"));
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

  /**
   * TC-UROL-007 (v0.0.7): projectId 任意 BIGINT 不校验（占位语义）。
   *
   * <p>v0.0.8 retrofit: the placeholder is now validated. Test seeds a real Project first so the
   * "arbitrary BIGINT" scenario is realized via an existing id.
   */
  @Test
  void post_arbitraryProjectId_accepted() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    // v0.0.8: project must now exist; seed one and use its id.
    com.rainier.project.domain.Project p = new com.rainier.project.domain.Project();
    p.setCode("PROJ-UR-V07");
    p.setName("seed");
    p.setStatus(com.rainier.project.domain.ProjectStatus.PLANNING);
    p.setOwnerUserId(userId);
    p.setEnabled(true);
    Long projectId = projectRepo.saveAndFlush(p).getId();

    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId));
  }

  // ===================== v0.0.8 projectId 激活 + 富化 (TC-URLP-001/002/003) ===================

  private Long createProject(Long ownerId, String code, String name) {
    com.rainier.project.domain.Project p = new com.rainier.project.domain.Project();
    p.setCode(code);
    p.setName(name);
    p.setStatus(com.rainier.project.domain.ProjectStatus.PLANNING);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  /** TC-URLP-001: POST 含 projectId 存在 → 富化. */
  @Test
  void post_withExistingProjectId_returnsEnriched() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    Long projectId = createProject(userId, "PROJ-UR-1", "采购系统改造");
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.projectName").value("采购系统改造"))
        .andExpect(jsonPath("$.projectCode").value("PROJ-UR-1"));
  }

  /** TC-URLP-002: POST 含 projectId 不存在 → 400. */
  @Test
  void post_unknownProjectId_returns400() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.put("projectId", 999_999L);
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("project not found")));
  }

  /** TC-URLP-003: POST 含 projectId=null → 公司级 hat 保留. */
  @Test
  void post_nullProjectId_keepsCompanyWideHat() throws Exception {
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
        .andExpect(jsonPath("$.projectId").isEmpty())
        .andExpect(jsonPath("$.projectName").isEmpty());
  }
}
