/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Integration tests for {@link TaskController} DELETE. Covers TC-TSK-015. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerDeleteTest {

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

  /** TC-TSK-015: 软删 Task + 后续 GET 404. */
  @Test
  void delete_softDeletes_returns204AndGet404() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long userId = userRepo.saveAndFlush(u).getId();
    Project p = new Project();
    p.setCode("PROJ-D");
    p.setName("x");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(userId);
    p.setEnabled(true);
    Long projectId = projectRepo.saveAndFlush(p).getId();
    ObjectNode body = json.createObjectNode();
    body.put("code", "TASK-D");
    body.put("title", "x");
    body.put("projectId", projectId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/api/tasks/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/tasks/" + id)).andExpect(status().isNotFound());
  }
}
