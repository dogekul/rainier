/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

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

/**
 * v0.0.81 story-review-ui: PUT 字段缺失不清空 reviewer (patch-like semantics).
 *
 * <p>Covers TC-SRU-001 / TC-SRU-002 / TC-SRU-003.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryUpdateNoReviewerWipeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long owner;
  private Long reviewer;
  private Long otherReviewer;
  private Long sprintId;

  @BeforeEach
  void setup() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    owner = createUser("alice-sru", "Alice");
    reviewer = createUser("anna-sru", "Anna Architect");
    otherReviewer = createUser("bob-sru", "Bob Architect");
    Long projectId = createProject(owner, "PROJ-SRU");
    Long reqId = createRequirement(owner, projectId, "REQ-SRU");
    sprintId = createSprint(reqId, owner, "SPR-SRU");
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

  private long createStoryWithReviewer(String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "评审字段 story");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", owner);
    body.put("reviewerUserId", reviewer);
    body.put("reviewStatus", "PENDING");
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

  /** PUT body without reviewer keys → must keep existing reviewer (the bug fix). */
  @Test
  void put_omitsReviewerFields_keepsExistingReviewer() throws Exception {
    long id = createStoryWithReviewer("STR-SRU-1");

    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-SRU-1");
    body.put("title", "更新后标题");
    body.put("status", "READY");
    body.put("priority", "HIGH");
    body.put("ownerUserId", owner);
    // intentionally NO reviewerUserId / reviewStatus keys.

    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("更新后标题"))
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer))
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
  }

  /** PUT body with explicit null → clears reviewer (legacy semantics preserved when explicit). */
  @Test
  void put_explicitNullReviewerFields_clearsReviewer() throws Exception {
    long id = createStoryWithReviewer("STR-SRU-2");

    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-SRU-2");
    body.put("title", "评审字段 story");
    body.put("status", "READY");
    body.put("priority", "HIGH");
    body.put("ownerUserId", owner);
    body.putNull("reviewerUserId");
    body.putNull("reviewStatus");

    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewerUserId").doesNotExist())
        .andExpect(jsonPath("$.reviewStatus").doesNotExist());
  }

  /** PUT body with explicit new reviewer → replaces. */
  @Test
  void put_explicitNewReviewer_replaces() throws Exception {
    long id = createStoryWithReviewer("STR-SRU-3");

    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-SRU-3");
    body.put("title", "评审字段 story");
    body.put("status", "READY");
    body.put("priority", "HIGH");
    body.put("ownerUserId", owner);
    body.put("reviewerUserId", otherReviewer);

    mockMvc
        .perform(
            put("/api/stories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewerUserId").value(otherReviewer))
        // reviewStatus key NOT sent → preserved from create-time PENDING.
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
  }
}
