/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
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

/** v0.0.82 task-review: review fields + POST /review endpoint. Covers TC-TREV-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskReviewTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long assignee;
  private Long reviewer;
  private Long projectId;

  @BeforeEach
  void setup() {
    taskRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    assignee = createUser("alice-trev", "Alice");
    reviewer = createUser("anna-trev", "Anna Architect");
    projectId = createProject(assignee, "PROJ-TREV");
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

  private ObjectNode taskBody(String code) {
    ObjectNode b = json.createObjectNode();
    b.put("code", code);
    b.put("title", "review test task");
    b.put("projectId", projectId);
    return b;
  }

  private long createTask(String code, Long reviewerId, String reviewStatus) throws Exception {
    ObjectNode body = taskBody(code);
    if (reviewerId != null) {
      body.put("reviewerUserId", reviewerId);
    }
    if (reviewStatus != null) {
      body.put("reviewStatus", reviewStatus);
    }
    String resp =
        mockMvc
            .perform(
                post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(resp).get("id").asLong();
  }

  /** TC-TREV-001: create with reviewer + PENDING → enriched reviewerName. */
  @Test
  void create_withReviewer_returns201Enriched() throws Exception {
    ObjectNode body = taskBody("TASK-TREV-1");
    body.put("reviewerUserId", reviewer);
    body.put("reviewStatus", "PENDING");
    mockMvc
        .perform(post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer))
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"))
        .andExpect(jsonPath("$.reviewerName").value("Anna Architect"));
  }

  /** TC-TREV-002: review APPROVED → status updated, reviewer kept. */
  @Test
  void review_approved_setsStatusKeepsReviewer() throws Exception {
    long id = createTask("TASK-TREV-2", reviewer, "PENDING");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "APPROVED");
    mockMvc
        .perform(
            post("/api/tasks/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewStatus").value("APPROVED"))
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer));
  }

  /** TC-TREV-003: REJECTED requires reason → 400 without, 200 with; closeReason persisted. */
  @Test
  void review_rejectedRequiresReason() throws Exception {
    long id = createTask("TASK-TREV-3", reviewer, "PENDING");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECTED");
    mockMvc
        .perform(
            post("/api/tasks/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("reason required for REJECTED")));

    body.put("reason", "重写");
    mockMvc
        .perform(
            post("/api/tasks/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewStatus").value("REJECTED"))
        .andExpect(jsonPath("$.closeReason").value("重写"));
  }

  /** TC-TREV-004: invalid decision → 400. */
  @Test
  void review_invalidDecision_returns400() throws Exception {
    long id = createTask("TASK-TREV-4", reviewer, "PENDING");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "MAYBE");
    mockMvc
        .perform(
            post("/api/tasks/" + id + "/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid decision")));
  }

  /** unknown task → 404. */
  @Test
  void review_unknownTask_returns404() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("decision", "APPROVED");
    mockMvc
        .perform(
            post("/api/tasks/999999/review")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isNotFound());
  }
}
