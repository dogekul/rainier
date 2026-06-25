/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/**
 * v0.0.82 task-review: PUT /api/tasks/{id} 不带 reviewer 字段不清空 (patch-like). Covers TC-TREV-005.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskUpdateNoReviewerWipeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long owner;
  private Long reviewer;
  private Long otherReviewer;
  private Long projectId;

  @BeforeEach
  void setup() {
    taskRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    owner = createUser("alice-tru", "Alice");
    reviewer = createUser("anna-tru", "Anna");
    otherReviewer = createUser("bob-tru", "Bob");
    projectId = createProject(owner, "PROJ-TRU");
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

  private long createTaskWithReviewer(String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "task with reviewer");
    body.put("projectId", projectId);
    body.put("reviewerUserId", reviewer);
    body.put("reviewStatus", "PENDING");
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

  @Test
  void put_omitsReviewerFields_keepsExistingReviewer() throws Exception {
    long id = createTaskWithReviewer("TASK-TRU-1");

    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-TRU-1");
    body.put("title", "更新后标题");
    body.put("status", "IN_PROGRESS");
    body.put("priority", "HIGH");
    // intentionally NO reviewerUserId / reviewStatus keys

    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("更新后标题"))
        .andExpect(jsonPath("$.reviewerUserId").value(reviewer))
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
  }

  @Test
  void put_explicitNullReviewerFields_clearsReviewer() throws Exception {
    long id = createTaskWithReviewer("TASK-TRU-2");

    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-TRU-2");
    body.put("title", "task with reviewer");
    body.put("status", "IN_PROGRESS");
    body.put("priority", "HIGH");
    body.putNull("reviewerUserId");
    body.putNull("reviewStatus");

    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewerUserId").doesNotExist())
        .andExpect(jsonPath("$.reviewStatus").doesNotExist());
  }

  @Test
  void put_explicitNewReviewer_replaces() throws Exception {
    long id = createTaskWithReviewer("TASK-TRU-3");

    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-TRU-3");
    body.put("title", "task with reviewer");
    body.put("status", "IN_PROGRESS");
    body.put("priority", "HIGH");
    body.put("reviewerUserId", otherReviewer);

    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewerUserId").value(otherReviewer))
        // reviewStatus key NOT sent → preserved from create-time PENDING.
        .andExpect(jsonPath("$.reviewStatus").value("PENDING"));
  }
}
