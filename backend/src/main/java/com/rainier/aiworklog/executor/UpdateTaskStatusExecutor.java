/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.executor;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.common.exception.BadRequestException;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import org.springframework.stereotype.Component;

/**
 * F1 (v0.0.100) — executes {@code UPDATE_TASK_STATUS} AI proposals by setting the target
 * {@link Task#status} to {@code DONE} and returning a JSON snapshot containing the prior status so
 * the change can be reversed.
 *
 * <p>Target task id is read from {@link AiWorkLog#getTargetId()} (populated by
 * {@code StatusSyncService} from {@code Event.extractedEntityId}).
 */
@Component
public class UpdateTaskStatusExecutor implements DecisionExecutor {

  static final String ACTION = "UPDATE_TASK_STATUS";

  private final TaskRepository taskRepo;

  public UpdateTaskStatusExecutor(TaskRepository taskRepo) {
    this.taskRepo = taskRepo;
  }

  @Override
  public boolean supports(AiWorkLog log) {
    return log != null && ACTION.equals(log.getAction());
  }

  @Override
  public ExecutorResult execute(AiWorkLog log) {
    Long taskId = log.getTargetId();
    if (taskId == null) {
      throw new BadRequestException("UPDATE_TASK_STATUS executor: targetId is required");
    }
    Task task =
        taskRepo
            .findById(taskId)
            .orElseThrow(
                () -> new BadRequestException("task not found for executor: id=" + taskId));
    String oldStatus = task.getStatus();
    String newStatus = TaskStatus.DONE;
    task.setStatus(newStatus);
    taskRepo.saveAndFlush(task);
    return ExecutorResult.ok(buildSnapshot(taskId, oldStatus, newStatus));
  }

  @Override
  public ExecutorResult reverse(AiWorkLog log, String snapshotJson) {
    if (snapshotJson == null || snapshotJson.trim().isEmpty()) {
      throw new BadRequestException("reverse: snapshot is empty");
    }
    Long taskId = extractLong(snapshotJson, "taskId");
    String oldStatus = extractString(snapshotJson, "oldStatus");
    if (taskId == null || oldStatus == null) {
      throw new BadRequestException("reverse: snapshot missing taskId/oldStatus");
    }
    Task task =
        taskRepo
            .findById(taskId)
            .orElseThrow(
                () -> new BadRequestException("task not found for reverse: id=" + taskId));
    task.setStatus(oldStatus);
    taskRepo.saveAndFlush(task);
    return ExecutorResult.ok(null);
  }

  // ---- tiny ad-hoc JSON helpers (Java 8, no extra deps) ----

  static String buildSnapshot(Long taskId, String oldStatus, String newStatus) {
    StringBuilder sb = new StringBuilder(64);
    sb.append("{\"taskId\":").append(taskId);
    sb.append(",\"oldStatus\":\"").append(escape(oldStatus)).append("\"");
    sb.append(",\"newStatus\":\"").append(escape(newStatus)).append("\"}");
    return sb.toString();
  }

  private static String escape(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static Long extractLong(String json, String key) {
    String token = "\"" + key + "\":";
    int i = json.indexOf(token);
    if (i < 0) {
      return null;
    }
    int start = i + token.length();
    int end = start;
    while (end < json.length()) {
      char c = json.charAt(end);
      if (c == ',' || c == '}' || c == ' ') {
        break;
      }
      end++;
    }
    try {
      return Long.parseLong(json.substring(start, end).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static String extractString(String json, String key) {
    String token = "\"" + key + "\":\"";
    int i = json.indexOf(token);
    if (i < 0) {
      return null;
    }
    int start = i + token.length();
    int end = json.indexOf('"', start);
    if (end < 0) {
      return null;
    }
    return json.substring(start, end);
  }
}
