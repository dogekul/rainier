/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for POST {@link RequirementController}. Covers TC-REQ-001..003. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementRepository repo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
    demandRepo.deleteAll();
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  /** TC-REQ-001: 最小 payload + 默认值。 */
  @Test
  void post_minimalPayload_returns201WithDefaults() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-001");
    body.put("title", "加速采购下单");
    body.put("description", "降低响应时长");
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/requirements/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value("REQ-001"))
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.complexity").isEmpty())
        .andExpect(jsonPath("$.projectId").isEmpty());
  }

  /** TC-REQ-002: code 全局唯一性冲突。 */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-001");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-REQ-003: ownerUser 不存在 → 400. */
  @Test
  void post_unknownOwner_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-001");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }

  // ===================== v0.0.8 projectId 激活 + 富化 (TC-REQP-001/002/003) =====================

  @Autowired private com.rainier.project.repository.ProjectRepository projectRepo;

  private Long createProject(Long ownerId) {
    com.rainier.project.domain.Project p = new com.rainier.project.domain.Project();
    p.setCode("PROJ-RP");
    p.setName("采购系统改造");
    p.setStatus(com.rainier.project.domain.ProjectStatus.PLANNING);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  /** TC-REQP-001: POST 含 projectId 存在 → 201 + 富化. */
  @Test
  void post_withExistingProjectId_returnsEnriched() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId);
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-RP-1");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.projectName").value("采购系统改造"))
        .andExpect(jsonPath("$.projectCode").value("PROJ-RP"));
  }

  /** TC-REQP-002: POST 含 projectId 不存在 → 400. */
  @Test
  void post_unknownProjectId_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-RP-2");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    body.put("projectId", 999_999L);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("project not found")));
  }

  /** TC-REQP-003: POST 含 projectId=null 兼容. */
  @Test
  void post_nullProjectId_returns201Compatible() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-RP-3");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    body.putNull("projectId");
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").isEmpty())
        .andExpect(jsonPath("$.projectName").isEmpty())
        .andExpect(jsonPath("$.projectCode").isEmpty());
  }
}
