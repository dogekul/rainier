/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.ai.domain.AiErrorStatus;
import com.rainier.ai.repository.AiErrorRepository;
import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
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

/** F1 (v0.0.100) — POST /api/ai-work-logs/{id}/reverse. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiWorkLogReverseControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AiWorkLogRepository logRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private AiErrorRepository aiErrorRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long projectId;

  @BeforeEach
  void cleanDb() {
    aiErrorRepo.deleteAll();
    logRepo.deleteAll();
    taskRepo.deleteAll();
    projectRepo.deleteAll();

    User owner = new User();
    owner.setLoginName("f1r-owner-" + System.nanoTime());
    owner.setName("f1r owner");
    owner.setIsInternal(true);
    owner.setEnabled(true);
    Long ownerId = userRepo.saveAndFlush(owner).getId();

    Project p = new Project();
    p.setCode("F1R-" + System.nanoTime());
    p.setName("f1 reverse ctl");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    projectId = projectRepo.saveAndFlush(p).getId();
  }

  private Task newTask(String status) {
    Task t = new Task();
    t.setCode("F1R-T-" + System.nanoTime());
    t.setTitle("t");
    t.setStatus(status);
    t.setPriority(Priority.MEDIUM);
    t.setProjectId(projectId);
    return taskRepo.saveAndFlush(t);
  }

  private Long acceptedLogFor(Task task) throws Exception {
    AiWorkLog a = new AiWorkLog();
    a.setAgentType("STATUS_SYNC");
    a.setAction("UPDATE_TASK_STATUS");
    a.setTargetType("TASK");
    a.setTargetId(task.getId());
    a.setSummary("s");
    a.setEvidence("ev");
    a.setStatus(AiWorkLogStatus.PROPOSED);
    Long id = logRepo.saveAndFlush(a).getId();
    ObjectNode body = json.createObjectNode();
    body.put("decision", "ACCEPTED");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
    return id;
  }

  @Test
  void reverse_restoresTask_revivesLog_andRecordsAiError() throws Exception {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    long aiErrBefore = aiErrorRepo.count();
    Long logId = acceptedLogFor(t);

    mockMvc
        .perform(post("/api/ai-work-logs/" + logId + "/reverse"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(AiWorkLogStatus.PROPOSED))
        .andExpect(jsonPath("$.reverseSnapshot").doesNotExist())
        .andExpect(jsonPath("$.reversedAt").isNotEmpty());

    Task reloaded = taskRepo.findById(t.getId()).orElseThrow(() -> new AssertionError("missing"));
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(aiErrorRepo.count()).isEqualTo(aiErrBefore + 1);
    assertThat(
            aiErrorRepo.findAll().stream()
                .anyMatch(e -> AiErrorStatus.OPEN.equals(e.getStatus())))
        .isTrue();
  }

  @Test
  void reverse_nonAcceptedLog_returns400() throws Exception {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    AiWorkLog a = new AiWorkLog();
    a.setAgentType("STATUS_SYNC");
    a.setAction("UPDATE_TASK_STATUS");
    a.setTargetType("TASK");
    a.setTargetId(t.getId());
    a.setSummary("s");
    a.setEvidence("ev");
    a.setStatus(AiWorkLogStatus.PROPOSED);
    Long id = logRepo.saveAndFlush(a).getId();

    mockMvc.perform(post("/api/ai-work-logs/" + id + "/reverse")).andExpect(status().isBadRequest());
  }

  @Test
  void reverse_acceptedButNoSnapshot_returns400() throws Exception {
    AiWorkLog a = new AiWorkLog();
    a.setAgentType("STATUS_SYNC");
    a.setAction("UPDATE_TASK_STATUS");
    a.setTargetType("TASK");
    a.setTargetId(null); // executor will throw → snapshot remains null
    a.setSummary("s");
    a.setEvidence("ev");
    a.setStatus(AiWorkLogStatus.PROPOSED);
    Long id = logRepo.saveAndFlush(a).getId();

    ObjectNode body = json.createObjectNode();
    body.put("decision", "ACCEPTED");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());

    mockMvc.perform(post("/api/ai-work-logs/" + id + "/reverse")).andExpect(status().isBadRequest());
  }

  @Test
  void reverse_twice_secondReturns400() throws Exception {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    Long logId = acceptedLogFor(t);
    mockMvc.perform(post("/api/ai-work-logs/" + logId + "/reverse")).andExpect(status().isOk());
    mockMvc
        .perform(post("/api/ai-work-logs/" + logId + "/reverse"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void reverse_unknownId_returns404() throws Exception {
    mockMvc
        .perform(post("/api/ai-work-logs/9999999/reverse"))
        .andExpect(status().isNotFound());
  }
}
