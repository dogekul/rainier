/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

import static org.hamcrest.Matchers.startsWith;
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
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link TaskController} PUT. Covers TC-TSK-012, 013, 014. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerUpdateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
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

  private Long createTask(Long projectId, Long assigneeUserId, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "T");
    body.put("projectId", projectId);
    if (assigneeUserId != null) body.put("assigneeUserId", assigneeUserId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-TSK-012: 更新 status + assignee 转移 + 富化跟随. */
  @Test
  void put_updateStatusAndTransferAssignee_returns200() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long lili = createUser("lili", "黎立");
    Long projectId = createProject(alice, "PROJ-U1");
    Long id = createTask(projectId, null, "TASK-U1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-U1");
    body.put("title", "T");
    body.put("status", "IN_PROGRESS");
    body.put("priority", "HIGH");
    body.put("assigneeUserId", lili);
    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.priority").value("HIGH"))
        .andExpect(jsonPath("$.assigneeUserId").value(lili))
        .andExpect(jsonPath("$.assigneeName").value("黎立"))
        .andExpect(jsonPath("$.assigneeLoginName").value("lili"));
  }

  /** TC-TSK-013: 改 assigneeUserId 至 null (unassign). */
  @Test
  void put_unassignByNull_returns200() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long projectId = createProject(alice, "PROJ-U2");
    Long id = createTask(projectId, alice, "TASK-U2");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-U2");
    body.put("title", "T");
    body.put("status", "TODO");
    body.put("priority", "MEDIUM");
    body.putNull("assigneeUserId");
    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.assigneeUserId").doesNotExist())
        .andExpect(jsonPath("$.assigneeName").doesNotExist())
        .andExpect(jsonPath("$.assigneeLoginName").doesNotExist());
  }

  /** TC-TSK-014: PUT 新 assigneeUserId 不存在 → 400. */
  @Test
  void put_unknownNewAssigneeUserId_returns400() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long projectId = createProject(alice, "PROJ-U3");
    Long id = createTask(projectId, null, "TASK-U3");
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-U3");
    body.put("title", "T");
    body.put("status", "TODO");
    body.put("priority", "MEDIUM");
    body.put("assigneeUserId", 999_999L);
    mockMvc
        .perform(
            put("/api/tasks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("assignee user not found")));
  }
}
