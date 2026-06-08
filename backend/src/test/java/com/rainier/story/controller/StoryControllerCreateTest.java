/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

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
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.repository.StoryRepository;
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

/** Integration tests for {@link StoryController} POST. Covers TC-STR-001..009. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
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
    r.setTitle("登录流程");
    r.setOwnerUserId(ownerId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long createSprint(Long reqId, Long ownerId, String code) {
    Sprint sp = new Sprint();
    sp.setCode(code);
    sp.setName("Phase 1");
    sp.setStatus(SprintStatus.PLANNING);
    sp.setRequirementId(reqId);
    sp.setOwnerUserId(ownerId);
    return sprintRepo.saveAndFlush(sp).getId();
  }

  /** TC-STR-001 (v0.0.10): minimal payload + projectId 二段继承 + sprint/requirement/project 全富化. */
  @Test
  void post_minimalPayload_inheritsProjectId_returnsEnriched() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-1");
    Long reqId = createRequirement(userId, projectId, "REQ-1");
    Long sprintId = createSprint(reqId, userId, "SPR-1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-001");
    body.put("title", "用户登录页");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/stories/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.status").value("DRAFT"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.complexity").doesNotExist())
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"))
        .andExpect(jsonPath("$.requirementCode").value("REQ-1"))
        .andExpect(jsonPath("$.requirementTitle").value("登录流程"))
        .andExpect(jsonPath("$.projectName").value("Apollo"))
        .andExpect(jsonPath("$.projectCode").value("PROJ-1"));
  }

  /** TC-STR-002: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-2");
    Long reqId = createRequirement(userId, projectId, "REQ-2");
    Long sprintId = createSprint(reqId, userId, "SPR-2");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-DUP");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-STR-SPR-002 (v0.0.10): sprintId 不存在 → 400 "sprint not found". */
  @Test
  void post_unknownSprintId_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-X");
    body.put("title", "x");
    body.put("sprintId", 999L);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("sprint not found")));
  }

  /** TC-STR-004: ownerUserId 不存在 → 400. */
  @Test
  void post_unknownOwnerUserId_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-4");
    Long reqId = createRequirement(userId, projectId, "REQ-4");
    Long sprintId = createSprint(reqId, userId, "SPR-4");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-Y");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }

  /** TC-STR-005: 非法 status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-5");
    Long reqId = createRequirement(userId, projectId, "REQ-5");
    Long sprintId = createSprint(reqId, userId, "SPR-5");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-Z");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-STR-006: 非法 priority → 400. */
  @Test
  void post_invalidPriority_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-6");
    Long reqId = createRequirement(userId, projectId, "REQ-6");
    Long sprintId = createSprint(reqId, userId, "SPR-6");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-PRIO");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    body.put("priority", "EXTREME");
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid priority")));
  }

  /** TC-STR-007: 非法 complexity → 400. */
  @Test
  void post_invalidComplexity_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-7");
    Long reqId = createRequirement(userId, projectId, "REQ-7");
    Long sprintId = createSprint(reqId, userId, "SPR-7");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-CPLX");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    body.put("complexity", "XXL");
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid complexity")));
  }

  /** TC-STR-008: 缺必填字段 → 400 fieldErrors. */
  @Test
  void post_missingRequiredFields_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-MISS");
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("title")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("sprintId")))
        .andExpect(
            jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-STR-009: createBy 自动注入登录 username. */
  @Test
  void post_createBy_autoInjectedByAuditor() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-CB");
    Long reqId = createRequirement(userId, projectId, "REQ-CB");
    Long sprintId = createSprint(reqId, userId, "SPR-CB");
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-CB");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }
}
