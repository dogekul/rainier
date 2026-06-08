/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link SprintController} GET/PUT. Covers TC-SPR-009..014. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintControllerQueryTest {

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
    r.setTitle("x");
    r.setOwnerUserId(ownerId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long createSprint(Long ownerId, Long reqId, String code, String status) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "S");
    body.put("requirementId", reqId);
    body.put("ownerUserId", ownerId);
    if (status != null) {
      body.put("status", status);
    }
    MvcResult res =
        mockMvc
            .perform(
                post("/api/sprints")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-SPR-009: GET 详情完整字段集 + 富化. */
  @Test
  void get_existingId_returnsFullDetailAndEnriched() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-Q1");
    Long reqId = createRequirement(userId, projectId, "REQ-1");
    Long id = createSprint(userId, reqId, "SPR-Q1", null);
    mockMvc
        .perform(get("/api/sprints/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.requirementCode").value("REQ-1"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.storyCount").value(0));
  }

  /** TC-SPR-010: 按 requirementId 过滤. */
  @Test
  void getList_filterByRequirementId_returnsOnlyMatching() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-F1");
    Long reqA = createRequirement(userId, projectId, "REQ-A");
    Long reqB = createRequirement(userId, projectId, "REQ-B");
    createSprint(userId, reqA, "SPR-F1", null);
    createSprint(userId, reqA, "SPR-F2", null);
    createSprint(userId, reqB, "SPR-F3", null);
    mockMvc
        .perform(get("/api/sprints?requirementId=" + reqA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].requirementId", everyItem(is(reqA.intValue()))));
  }

  /** TC-SPR-011: 按 status 过滤. */
  @Test
  void getList_filterByStatus_returnsOnlyMatching() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-F2");
    Long reqId = createRequirement(userId, projectId, "REQ-S");
    createSprint(userId, reqId, "SPR-S1", "ACTIVE");
    createSprint(userId, reqId, "SPR-S2", "ACTIVE");
    createSprint(userId, reqId, "SPR-S3", "COMPLETED");
    mockMvc
        .perform(get("/api/sprints?status=ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
  }

  /** TC-SPR-012: 更新 status + goal. */
  @Test
  void put_updatesStatusAndGoal() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-U1");
    Long reqId = createRequirement(userId, projectId, "REQ-U");
    Long id = createSprint(userId, reqId, "SPR-U1", null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-U1");
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", userId);
    body.put("goal", "交付首版登录");
    mockMvc
        .perform(
            put("/api/sprints/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.goal").value("交付首版登录"));
  }

  /** TC-SPR-013: PUT 改 ownerUserId 富化跟随. */
  @Test
  void put_transferOwner_enrichmentFollows() throws Exception {
    Long userA = createUser("alice", "Alice");
    Long userB = createUser("lili", "黎立");
    Long projectId = createProject(userA, "PROJ-U2");
    Long reqId = createRequirement(userA, projectId, "REQ-U2");
    Long id = createSprint(userA, reqId, "SPR-OWN", null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-OWN");
    body.put("name", "X");
    body.put("status", "PLANNING");
    body.put("ownerUserId", userB);
    mockMvc
        .perform(
            put("/api/sprints/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerUserId").value(userB))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }

  /** TC-SPR-014: PUT 新 ownerUserId 不存在 → 400. */
  @Test
  void put_unknownNewOwner_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-U3");
    Long reqId = createRequirement(userId, projectId, "REQ-U3");
    Long id = createSprint(userId, reqId, "SPR-U3", null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-U3");
    body.put("name", "X");
    body.put("status", "PLANNING");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            put("/api/sprints/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }
}
