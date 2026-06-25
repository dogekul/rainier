/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk.rules;

import com.rainier.risk.RiskContext;
import com.rainier.risk.RiskFinding;
import com.rainier.risk.RiskRule;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * WARN every task whose {@code dueDate} is strictly before {@code ctx.now} and whose status is not
 * {@link TaskStatus#DONE} (nor terminal {@code CANCELLED}). Scope-bounded by {@code
 * ctx.projectIds}.
 */
@Component
public class OverdueTaskRule implements RiskRule {

  private final TaskRepository taskRepo;

  public OverdueTaskRule(TaskRepository taskRepo) {
    this.taskRepo = taskRepo;
  }

  @Override
  public String name() {
    return "OverdueTaskRule";
  }

  @Override
  public List<RiskFinding> evaluate(RiskContext ctx) {
    List<RiskFinding> out = new ArrayList<RiskFinding>();
    if (ctx == null || ctx.getProjectIds().isEmpty() || ctx.getNow() == null) {
      return out;
    }
    LocalDate today = ctx.getNow().toLocalDate();
    List<Task> tasks = taskRepo.findByProjectIdIn(ctx.getProjectIds());
    for (Task t : tasks) {
      if (t.getDueDate() == null) {
        continue;
      }
      if (!t.getDueDate().isBefore(today)) {
        continue;
      }
      String s = t.getStatus();
      if (TaskStatus.DONE.equals(s) || TaskStatus.CANCELLED.equals(s)) {
        continue;
      }
      String msg =
          "Task #" + t.getId() + " 已过期 (dueDate=" + t.getDueDate() + ", status=" + s + ")";
      out.add(new RiskFinding(RiskFinding.LEVEL_WARN, msg, "TASK", t.getId(), name()));
    }
    return out;
  }
}
