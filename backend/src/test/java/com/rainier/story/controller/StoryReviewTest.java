/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

import static org.hamcrest.Matchers.startsWith;
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

/** v0.0.39 review fields + decision endpoint. Covers TC-REVQ-001..010. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryReviewTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long owner;
  private Long reviewer;
  private Long sprintId;

  @BeforeEach
  void setup() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    owner = createUser("alice", "Alice");
    reviewer = createUser("arch", "Architect Anna");
    Long projectId = createProject(owner, "PROJ-REV");
    Long reqId = createRequirement(owner, projectId, "REQ-REV");
    sprintId = createSprint(reqId, owner, "SPR-REV");
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

  private ObjectNode storyBody(String code) {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "评审字段 story");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", owner);
    return body;
  }

  /** Create a story and return its id. */
  private long createStory(String code, Long reviewerId, String reviewStatus) throws Exception {
    ObjectNode body = storyBody(code);
    if (reviewerId != null) {
      body.put("reviewerUserId", reviewerId);
    }
    if (reviewStatus != null) {
      body.put("reviewStatus", reviewStatus);
    }
    String resp =
        mockMvc
            .perform(
                post("/api/stories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(resp).get("id").asLong();
  }

  /** TC-REVQ-001: create with reviewerUserId + PENDING → 201 + enriched reviewerName. */
  @Test
  void create_withReviewer_returns201Enriched() throws Exception {
    ObjectNode body = storyBody("STR-REV-1");
    body.put("reviewerUserId", reviewer);
    body.put("reviewStatus", "PENDING");
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer))
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"))
        .andExpect(jsonPath("$.reviewerName").value("Architect Anna"));
  }

  /** TC-REVQ-002: reviewerUserId not found → 400. */
  @Test
  void create_unknownReviewer_returns400() throws Exception {
    ObjectNode body = storyBody("STR-REV-2");
    body.put("reviewerUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("reviewer user not found")));
  }

  /** TC-REVQ-003: invalid reviewStatus → 400. */
  @Test
  void create_invalidReviewStatus_returns400() throws Exception {
    ObjectNode body = storyBody("STR-REV-3");
    body.put("reviewStatus", "MAYBE");
    mockMvc
        .perform(
            post("/api/stories").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid reviewStatus")));
  }

  /** TC-REVQ-004: create without review fields → 201 with nulls. */
  @Test
  void create_noReviewFields_returnsNulls() throws Exception {
    mockMvc
        .perform(
            post("/api/stories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(storyBody("STR-REV-4").toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reviewerUserId").doesNotExist())
        .andExpect(jsonPath("$.reviewStatus").doesNotExist());
  }

  /** TC-REVQ-005: review APPROVED → 200, reviewStatus updated, reviewer unchanged. */
  @Test
  void review_approved_setsStatusKeepsReviewer() throws Exception {
    long id = createStory("STR-REV-5", reviewer, "PENDING");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "APPROVED");
    mockMvc
        .perform(
            post("/api/stories/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer));
  }

  /** TC-REVQ-006: review REJECTED → 200. */
  @Test
  void review_rejected_setsStatus() throws Exception {
    long id = createStory("STR-REV-6", reviewer, "PENDING");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECTED");
    mockMvc
        .perform(
            post("/api/stories/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewStatus").value("REJECTED"));
  }

  /** TC-REVQ-007: invalid decision → 400. */
  @Test
  void review_invalidDecision_returns400() throws Exception {
    long id = createStory("STR-REV-7", reviewer, "PENDING");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "MEH");
    mockMvc
        .perform(
            post("/api/stories/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid decision")));
  }

  /** TC-REVQ-008: review unknown story → 404. */
  @Test
  void review_unknownStory_returns404() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("decision", "APPROVED");
    mockMvc
        .perform(
            post("/api/stories/999999/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isNotFound());
  }

  /** TC-REVQ-009: PUT update sets reviewer fields. */
  @Test
  void update_setsReviewFields() throws Exception {
    long id = createStory("STR-REV-9", null, null);
    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-REV-9");
    body.put("title", "评审字段 story");
    body.put("status", "READY");
    body.put("priority", "HIGH");
    body.put("ownerUserId", owner);
    body.put("reviewerUserId", reviewer);
    body.put("reviewStatus", "PENDING");
    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer))
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
  }
}
