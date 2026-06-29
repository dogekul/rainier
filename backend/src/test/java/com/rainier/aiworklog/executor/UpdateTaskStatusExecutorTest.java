/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.common.domain.Priority;
import com.rainier.common.exception.BadRequestException;
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

/** F1 (v0.0.100) — direct tests for {@link UpdateTaskStatusExecutor}. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UpdateTaskStatusExecutorTest {

  @Autowired private UpdateTaskStatusExecutor executor;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  private Long projectId;

  @BeforeEach
  void seedProject() {
    User owner = new User();
    owner.setLoginName("exec-owner-" + System.nanoTime());
    owner.setName("exec owner");
    owner.setIsInternal(true);
    owner.setEnabled(true);
    Long ownerId = userRepo.saveAndFlush(owner).getId();

    Project p = new Project();
    p.setCode("EXEC-P-" + System.nanoTime());
    p.setName("exec test project");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    projectId = projectRepo.saveAndFlush(p).getId();
  }

  private Task newTask(String status) {
    Task t = new Task();
    t.setCode("EXEC-T-" + System.nanoTime());
    t.setTitle("t");
    t.setStatus(status);
    t.setPriority(Priority.MEDIUM);
    t.setProjectId(projectId);
    return taskRepo.saveAndFlush(t);
  }

  @Test
  void supports_onlyUpdateTaskStatusAction() {
    AiWorkLog yes = new AiWorkLog();
    yes.setAction("UPDATE_TASK_STATUS");
    assertThat(executor.supports(yes)).isTrue();

    AiWorkLog no = new AiWorkLog();
    no.setAction("FLAG_RISK");
    assertThat(executor.supports(no)).isFalse();
  }

  @Test
  void execute_setsDone_andSnapshotsOldStatus() {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    AiWorkLog log = new AiWorkLog();
    log.setAction("UPDATE_TASK_STATUS");
    log.setTargetId(t.getId());

    ExecutorResult result = executor.execute(log);

    assertThat(result.isExecuted()).isTrue();
    assertThat(result.getSnapshot())
        .contains("\"taskId\":" + t.getId())
        .contains("\"oldStatus\":\"IN_PROGRESS\"")
        .contains("\"newStatus\":\"DONE\"");

    Task reloaded = taskRepo.findById(t.getId()).orElseThrow(() -> new AssertionError("missing"));
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
  }

  @Test
  void reverse_restoresOldStatus() {
    Task t = newTask(TaskStatus.IN_PROGRESS);
    AiWorkLog log = new AiWorkLog();
    log.setAction("UPDATE_TASK_STATUS");
    log.setTargetId(t.getId());
    ExecutorResult fwd = executor.execute(log);

    executor.reverse(log, fwd.getSnapshot());

    Task reloaded = taskRepo.findById(t.getId()).orElseThrow(() -> new AssertionError("missing"));
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void execute_missingTargetId_throws400() {
    AiWorkLog log = new AiWorkLog();
    log.setAction("UPDATE_TASK_STATUS");
    assertThatThrownBy(() -> executor.execute(log)).isInstanceOf(BadRequestException.class);
  }

  @Test
  void execute_unknownTask_throws400() {
    AiWorkLog log = new AiWorkLog();
    log.setAction("UPDATE_TASK_STATUS");
    log.setTargetId(999_999L);
    assertThatThrownBy(() -> executor.execute(log)).isInstanceOf(BadRequestException.class);
  }

  @Test
  void reverse_emptySnapshot_throws400() {
    AiWorkLog log = new AiWorkLog();
    log.setAction("UPDATE_TASK_STATUS");
    assertThatThrownBy(() -> executor.reverse(log, "")).isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> executor.reverse(log, null)).isInstanceOf(BadRequestException.class);
  }
}
