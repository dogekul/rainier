/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
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
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link StoryController} GET / PUT. Covers TC-STR-010..015. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository storyRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
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

  private Long createRequirement(Long ownerId, Long projectId, String code, String title) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle(title);
    r.setOwnerUserId(ownerId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long createStory(Long ownerId, Long reqId, String code, String status) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "S");
    body.put("requirementId", reqId);
    body.put("ownerUserId", ownerId);
    if (status != null) {
      body.put("status", status);
    }
    MvcResult res =
        mockMvc
            .perform(
                post("/api/stories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-STR-010: GET 详情完整字段集 + 富化. */
  @Test
  void get_existingId_returnsFullDetailAndEnriched() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-Q1");
    Long reqId = createRequirement(userId, projectId, "REQ-1", "登录流程");
    Long id = createStory(userId, reqId, "STR-Q1", null);
    MvcResult res =
        mockMvc
            .perform(get("/api/stories/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.requirementCode").value("REQ-1"))
            .andExpect(jsonPath("$.requirementTitle").value("登录流程"))
            .andExpect(jsonPath("$.ownerName").value("Alice"))
            // v0.0.9 Phase 5 Test-M2 fix: enrichment values must match on GET-detail path too,
            // not only on the create-path (TC-STR-001). A regression where Service short-circuits
            // enrich on GET would now fail visibly.
            .andExpect(jsonPath("$.ownerLoginName").value("alice"))
            .andExpect(jsonPath("$.projectName").value("Apollo"))
            .andExpect(jsonPath("$.projectCode").value("PROJ-Q1"))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "title",
      "description",
      "acceptanceCriteria",
      "status",
      "priority",
      "complexity",
      "requirementId",
      "requirementCode",
      "requirementTitle",
      "projectId",
      "projectName",
      "projectCode",
      "ownerUserId",
      "ownerName",
      "ownerLoginName",
      "closeReason",
      "createTime",
      "updateTime",
      "createBy",
      "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-STR-011: 按 requirementId 过滤列表. */
  @Test
  void getList_filterByRequirementId_returnsOnlyMatching() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-F1");
    Long reqA = createRequirement(userId, projectId, "REQ-A", "x");
    Long reqB = createRequirement(userId, projectId, "REQ-B", "x");
    createStory(userId, reqA, "STR-F1", null);
    createStory(userId, reqA, "STR-F2", null);
    createStory(userId, reqA, "STR-F3", null);
    createStory(userId, reqB, "STR-F4", null);
    createStory(userId, reqB, "STR-F5", null);
    mockMvc
        .perform(get("/api/stories?requirementId=" + reqA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.content[*].requirementId", everyItem(is(reqA.intValue()))));
  }

  /** TC-STR-012: 按 status 过滤列表. */
  @Test
  void getList_filterByStatus_returnsOnlyMatching() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-F2");
    Long reqId = createRequirement(userId, projectId, "REQ-S", "x");
    createStory(userId, reqId, "STR-S1", "IN_PROGRESS");
    createStory(userId, reqId, "STR-S2", "IN_PROGRESS");
    createStory(userId, reqId, "STR-S3", "DONE");
    mockMvc
        .perform(get("/api/stories?status=IN_PROGRESS"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("IN_PROGRESS"))));
  }

  /** TC-STR-013: 更新 status + priority + acceptanceCriteria. */
  @Test
  void put_updatesFields() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-U1");
    Long reqId = createRequirement(userId, projectId, "REQ-U", "x");
    Long id = createStory(userId, reqId, "STR-U1", null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-U1");
    body.put("title", "X");
    body.put("ownerUserId", userId);
    body.put("status", "IN_PROGRESS");
    body.put("priority", "HIGH");
    body.put("acceptanceCriteria", "用户能登录成功；错误密码弹提示");
    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.acceptanceCriteria").value("用户能登录成功；错误密码弹提示"));
  }

  /** TC-STR-014: PUT 改 ownerUserId 转移负责人 + 富化跟随. */
  @Test
  void put_transferOwner_enrichmentFollows() throws Exception {
    Long userA = createUser("alice", "Alice");
    Long userB = createUser("lili", "黎立");
    Long projectId = createProject(userA, "PROJ-U2");
    Long reqId = createRequirement(userA, projectId, "REQ-U2", "x");
    Long id = createStory(userA, reqId, "STR-OWN", null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-OWN");
    body.put("title", "X");
    body.put("ownerUserId", userB);
    body.put("status", "DRAFT");
    body.put("priority", "MEDIUM");
    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerUserId").value(userB))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }

  /** TC-STR-015: PUT 新 ownerUserId 不存在 → 400. */
  @Test
  void put_unknownNewOwner_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long projectId = createProject(userId, "PROJ-U3");
    Long reqId = createRequirement(userId, projectId, "REQ-U3", "x");
    Long id = createStory(userId, reqId, "STR-U3", null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-U3");
    body.put("title", "X");
    body.put("ownerUserId", 999_999L);
    body.put("status", "DRAFT");
    body.put("priority", "MEDIUM");
    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }
}
