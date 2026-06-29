/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.dto.AiWorkLogDetail;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * F1 (v0.0.100) — ACCEPTED now drives the matching {@link
 * com.rainier.aiworklog.executor.DecisionExecutor}; REJECTED still must not touch the entity.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiWorkLogServiceDecisionWithExecutorTest {

  @Autowired private AiWorkLogService service;
  @Autowired private AiWorkLogRepository logRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  private Long projectId;

  @BeforeEach
  void seedProject() {
    User owner = new User();
    owner.setLoginName("f1s-owner-" + System.nanoTime());
    owner.setName("f1s owner");
    owner.setIsInternal(true);
    owner.setEnabled(true);
    Long ownerId = userRepo.saveAndFlush(owner).getId();

    Project p = new Project();
    p.setCode("F1S-" + System.nanoTime());
    p.setName("f1 service test");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    projectId = projectRepo.saveAndFlush(p).getId();
  }

  private Task newTask(String status) {
    Task t = new Task();
    t.setCode("F1S-T-" + System.nanoTime());
    t.setTitle("t");
    t.setStatus(status);
    t.setPriority(Priority.MEDIUM);
    t.setProjectId(projectId);
    return taskRepo.saveAndFlush(t);
  }

  private AiWorkLog newProposedLog(Long taskId, String action) {
    AiWorkLog a = new AiWorkLog();
    a.setAgentType("STATUS_SYNC");
    a.setAction(action);
    a.setTargetType("TASK");
    a.setTargetId(taskId);
    a.setSummary("s");
    a.setEvidence("evidence");
    a.setStatus(AiWorkLogStatus.PROPOSED);
    return logRepo.saveAndFlush(a);
  }

  @Test
  void accept_updateTaskStatus_runsExecutor_andStoresSnapshot() {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    AiWorkLog log = newProposedLog(t.getId(), "UPDATE_TASK_STATUS");

    AiWorkLogDetail decided =
        service.decide(log.getId(), AiWorkLogStatus.ACCEPTED, null, "alice");

    assertThat(decided.getStatus()).isEqualTo(AiWorkLogStatus.ACCEPTED);
    assertThat(decided.getReverseSnapshot())
        .isNotNull()
        .contains("\"taskId\":" + t.getId())
        .contains("\"oldStatus\":\"IN_PROGRESS\"");

    Task reloaded = taskRepo.findById(t.getId()).orElseThrow(() -> new AssertionError("missing"));
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
  }

  @Test
  void reject_doesNotMutateEntity_noSnapshot() {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    AiWorkLog log = newProposedLog(t.getId(), "UPDATE_TASK_STATUS");

    AiWorkLogDetail decided =
        service.decide(log.getId(), AiWorkLogStatus.REJECTED, "误判", "alice");

    assertThat(decided.getStatus()).isEqualTo(AiWorkLogStatus.REJECTED);
    assertThat(decided.getReverseSnapshot()).isNull();
    assertThat(decided.getRejectReason()).isEqualTo("误判");

    Task reloaded = taskRepo.findById(t.getId()).orElseThrow(() -> new AssertionError("missing"));
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void accept_unknownAction_stillFlipsStatus_butNoSnapshot() {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    AiWorkLog log = newProposedLog(t.getId(), "DRAFT_WEEKLY");

    AiWorkLogDetail decided =
        service.decide(log.getId(), AiWorkLogStatus.ACCEPTED, null, "alice");

    assertThat(decided.getStatus()).isEqualTo(AiWorkLogStatus.ACCEPTED);
    assertThat(decided.getReverseSnapshot()).isNull();
    Task reloaded = taskRepo.findById(t.getId()).orElseThrow(() -> new AssertionError("missing"));
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void accept_executorException_doesNotBlock_statusFlips_snapshotNull() {
    // targetId is absent → UpdateTaskStatusExecutor throws BadRequestException; service swallows
    AiWorkLog log = newProposedLog(null, "UPDATE_TASK_STATUS");

    AiWorkLogDetail decided =
        service.decide(log.getId(), AiWorkLogStatus.ACCEPTED, null, "alice");

    assertThat(decided.getStatus()).isEqualTo(AiWorkLogStatus.ACCEPTED);
    assertThat(decided.getReverseSnapshot()).isNull();
  }
}
