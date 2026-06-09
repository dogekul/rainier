/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

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
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.repository.TaskRepository;
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

/**
 * Integration tests for {@link com.rainier.task.controller.TaskController} POST. Covers
 * TC-TSK-001..009 (basic CRUD) + TC-TSK-CONS-001..003 (cross-layer consistency).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TaskRepository taskRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  // ---- fixture helpers ----

  private Long createUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createProject(Long ownerId, String code, String name) {
    Project p = new Project();
    p.setCode(code);
    p.setName(name);
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

  /** Seeds Story with projectId set (mimicking v0.0.10 二段继承 from sprint→requirement). */
  private Long createStory(Long sprintId, Long projectId, Long ownerId, String code) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("用户登录页");
    s.setStatus(StoryStatus.DRAFT);
    s.setPriority(Priority.MEDIUM);
    s.setSprintId(sprintId);
    s.setProjectId(projectId);
    s.setOwnerUserId(ownerId);
    return storyRepo.saveAndFlush(s).getId();
  }

  // ---- TC-TSK-001..009 (basic create) ----

  /** TC-TSK-001: 最小 payload 创建 Task + 默认值 + projectName 富化. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndProjectEnrichment() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-1", "Apollo");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-001");
    body.put("title", "修登录页 bug");
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/tasks/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.status").value("TODO"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.projectName").value("Apollo"))
        .andExpect(jsonPath("$.projectCode").value("PROJ-1"))
        .andExpect(jsonPath("$.sprintId").doesNotExist())
        .andExpect(jsonPath("$.storyId").doesNotExist())
        .andExpect(jsonPath("$.assigneeUserId").doesNotExist())
        .andExpect(jsonPath("$.assigneeName").doesNotExist());
  }

  /** TC-TSK-002: 完整 payload 创建 Task w/ Sprint + Story + Assignee + 全富化. */
  @Test
  void post_fullPayload_returns201WithAllEnrichment() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long lili = createUser("lili", "黎立");
    Long projectId = createProject(alice, "PROJ-2", "Apollo");
    Long reqId = createRequirement(alice, projectId, "REQ-1");
    Long sprintId = createSprint(reqId, alice, "SPR-1");
    Long storyId = createStory(sprintId, projectId, alice, "STR-1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-002");
    body.put("title", "实现登录 API");
    body.put("projectId", projectId);
    body.put("sprintId", sprintId);
    body.put("storyId", storyId);
    body.put("assigneeUserId", lili);
    body.put("status", "IN_PROGRESS");
    body.put("priority", "HIGH");
    body.put("dueDate", "2026-07-15");
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.sprintCode").value("SPR-1"))
        .andExpect(jsonPath("$.sprintName").value("Phase 1"))
        .andExpect(jsonPath("$.storyCode").value("STR-1"))
        .andExpect(jsonPath("$.storyTitle").value("用户登录页"))
        .andExpect(jsonPath("$.assigneeName").value("黎立"))
        .andExpect(jsonPath("$.assigneeLoginName").value("lili"))
        .andExpect(jsonPath("$.dueDate").value("2026-07-15"));
  }

  /** TC-TSK-003: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-3", "x");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-DUP");
    body.put("title", "x");
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-TSK-004: projectId 不存在 → 400. */
  @Test
  void post_unknownProjectId_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-X");
    body.put("title", "x");
    body.put("projectId", 999L);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("project not found")));
  }

  /** TC-TSK-005: assigneeUserId 不存在 → 400. */
  @Test
  void post_unknownAssigneeUserId_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-5", "x");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-Y");
    body.put("title", "x");
    body.put("projectId", projectId);
    body.put("assigneeUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("assignee user not found")));
  }

  /** TC-TSK-006: 非法 status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-6", "x");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-Z");
    body.put("title", "x");
    body.put("projectId", projectId);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-TSK-007: 非法 priority → 400. */
  @Test
  void post_invalidPriority_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-7", "x");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-PR");
    body.put("title", "x");
    body.put("projectId", projectId);
    body.put("priority", "EXTREME");
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid priority")));
  }

  /** TC-TSK-008: 缺必填字段 → 400 fieldErrors. */
  @Test
  void post_missingRequiredFields_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-MISS");
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("title")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("projectId")));
  }

  /** TC-TSK-009: createBy 自动注入登录 username. */
  @Test
  void post_createBy_autoInjectedByAuditor() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-CB", "x");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-CB");
    body.put("title", "x");
    body.put("projectId", projectId);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }

  // ---- TC-TSK-CONS-001..003 (cross-layer consistency) ----

  /** TC-TSK-CONS-001: sprint 跨 project → 400 "sprint not in project". */
  @Test
  void post_sprintBelongsToDifferentProject_returns400() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long projectA = createProject(alice, "PROJ-A", "A");
    Long projectB = createProject(alice, "PROJ-B", "B");
    Long reqB = createRequirement(alice, projectB, "REQ-B");
    Long sprintB = createSprint(reqB, alice, "SPR-B"); // Sprint in project B
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-CONS-1");
    body.put("title", "x");
    body.put("projectId", projectA); // Task targets A
    body.put("sprintId", sprintB);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("sprint not in project")));
  }

  /** TC-TSK-CONS-002: story 跨 project → 400 "story not in project". */
  @Test
  void post_storyBelongsToDifferentProject_returns400() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long projectA = createProject(alice, "PROJ-CA", "A");
    Long projectB = createProject(alice, "PROJ-CB", "B");
    Long reqB = createRequirement(alice, projectB, "REQ-CB");
    Long sprintB = createSprint(reqB, alice, "SPR-CB");
    Long storyB = createStory(sprintB, projectB, alice, "STR-CB"); // Story in B
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-CONS-2");
    body.put("title", "x");
    body.put("projectId", projectA);
    body.put("storyId", storyB);
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("story not in project")));
  }

  /** TC-TSK-CONS-003: sprint+story 都 set 但 story 不属该 sprint → 400 "story not in sprint". */
  @Test
  void post_storyNotInSprint_whenBothSet_returns400() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long projectId = createProject(alice, "PROJ-CC", "x");
    Long reqId = createRequirement(alice, projectId, "REQ-CC");
    Long sprintA = createSprint(reqId, alice, "SPR-CC-A");
    Long sprintB = createSprint(reqId, alice, "SPR-CC-B");
    Long storyInB = createStory(sprintB, projectId, alice, "STR-CC");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-CONS-3");
    body.put("title", "x");
    body.put("projectId", projectId);
    body.put("sprintId", sprintA); // claim sprint A
    body.put("storyId", storyInB); // but story is in sprint B
    mockMvc
        .perform(
            post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("story not in sprint")));
  }
}
