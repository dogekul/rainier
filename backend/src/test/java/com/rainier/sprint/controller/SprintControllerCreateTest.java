/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.repository.SprintRepository;
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

/** Integration tests for {@link SprintController} POST. Covers TC-SPR-001..008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
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

  private Long createProject(Long ownerId, String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName("Apollo");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private Long createRequirement(Long ownerId, Long projectId, String code) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("登录");
    r.setOwnerUserId(ownerId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return requirementRepo.saveAndFlush(r).getId();
  }

  /** TC-SPR-001: 最小 payload + 默认 status + 富化 + storyCount=0. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndEnrichment() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId, "PROJ-1");
    Long reqId = createRequirement(userId, projectId, "REQ-1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-001");
    body.put("name", "Phase 1");
    body.put("requirementId", reqId);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/sprints/\\d+")))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.requirementCode").value("REQ-1"))
        .andExpect(jsonPath("$.requirementTitle").value("登录"))
        .andExpect(jsonPath("$.projectName").value("Apollo"))
        .andExpect(jsonPath("$.projectCode").value("PROJ-1"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"))
        .andExpect(jsonPath("$.storyCount").value(0));
  }

  /** TC-SPR-002: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId, "PROJ-2");
    Long reqId = createRequirement(userId, projectId, "REQ-2");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-DUP");
    body.put("name", "x");
    body.put("requirementId", reqId);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-SPR-003: requirementId 不存在 → 400. */
  @Test
  void post_unknownRequirementId_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-X");
    body.put("name", "x");
    body.put("requirementId", 999L);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("requirement not found")));
  }

  /** TC-SPR-004: ownerUserId 不存在 → 400. */
  @Test
  void post_unknownOwnerUserId_returns400() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId, "PROJ-4");
    Long reqId = createRequirement(userId, projectId, "REQ-4");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-Y");
    body.put("name", "x");
    body.put("requirementId", reqId);
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }

  /** TC-SPR-005: 非法 status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId, "PROJ-5");
    Long reqId = createRequirement(userId, projectId, "REQ-5");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-Z");
    body.put("name", "x");
    body.put("requirementId", reqId);
    body.put("ownerUserId", userId);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-SPR-006: 缺必填字段 → 400 fieldErrors. */
  @Test
  void post_missingRequiredFields_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-MISS");
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(
            jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("requirementId")))
        .andExpect(
            jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-SPR-007: createBy 自动注入. */
  @Test
  void post_createBy_autoInjectedByAuditor() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId, "PROJ-CB");
    Long reqId = createRequirement(userId, projectId, "REQ-CB");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-CB");
    body.put("name", "x");
    body.put("requirementId", reqId);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }

  /** TC-SPR-008: 时间字段不做一致性校验（end < start 仍 201 — 层级语义）. */
  @Test
  void post_timeFieldsNotCoherenceChecked_endBeforeStart_returns201() throws Exception {
    Long userId = createUser();
    Long projectId = createProject(userId, "PROJ-T");
    Long reqId = createRequirement(userId, projectId, "REQ-T");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-T");
    body.put("name", "x");
    body.put("requirementId", reqId);
    body.put("ownerUserId", userId);
    body.put("startDate", "2026-12-31");
    body.put("endDate", "2026-01-01");
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
  }
}
