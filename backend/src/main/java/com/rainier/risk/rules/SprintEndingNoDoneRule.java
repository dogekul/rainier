/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk.rules;

import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.risk.RiskContext;
import com.rainier.risk.RiskFinding;
import com.rainier.risk.RiskRule;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * WARN every Sprint whose {@code endDate} is within the next 3 days (inclusive of {@code now+3})
 * AND has zero tasks in {@link TaskStatus#DONE}. Sprints with {@code endDate=null} or already past
 * end ({@code endDate &lt; today}) are skipped — risk is forward-looking. A sprint with no tasks at
 * all is treated the same as a sprint with tasks but none DONE.
 */
@Component
public class SprintEndingNoDoneRule implements RiskRule {

  private static final int WINDOW_DAYS = 3;

  private final SprintRepository sprintRepo;
  private final RequirementRepository requirementRepo;
  private final TaskRepository taskRepo;

  public SprintEndingNoDoneRule(
      SprintRepository sprintRepo,
      RequirementRepository requirementRepo,
      TaskRepository taskRepo) {
    this.sprintRepo = sprintRepo;
    this.requirementRepo = requirementRepo;
    this.taskRepo = taskRepo;
  }

  @Override
  public String name() {
    return "SprintEndingNoDoneRule";
  }

  @Override
  public List<RiskFinding> evaluate(RiskContext ctx) {
    List<RiskFinding> out = new ArrayList<RiskFinding>();
    if (ctx == null || ctx.getProjectIds().isEmpty() || ctx.getNow() == null) {
      return out;
    }
    List<Requirement> reqs = requirementRepo.findByProjectIdIn(ctx.getProjectIds());
    if (reqs.isEmpty()) {
      return out;
    }
    Set<Long> reqIds = new HashSet<Long>();
    for (Requirement r : reqs) {
      reqIds.add(r.getId());
    }
    List<Sprint> sprints = sprintRepo.findByRequirementIdIn(reqIds);
    if (sprints.isEmpty()) {
      return out;
    }
    LocalDate today = ctx.getNow().toLocalDate();
    LocalDate windowEnd = today.plusDays(WINDOW_DAYS);

    Set<Long> candidateSprintIds = new HashSet<Long>();
    for (Sprint sp : sprints) {
      if (sp.getEndDate() == null) {
        continue;
      }
      if (sp.getEndDate().isBefore(today)) {
        continue;
      }
      if (sp.getEndDate().isAfter(windowEnd)) {
        continue;
      }
      candidateSprintIds.add(sp.getId());
    }
    if (candidateSprintIds.isEmpty()) {
      return out;
    }
    List<Task> tasks = taskRepo.findBySprintIdIn(candidateSprintIds);
    Map<Long, Boolean> hasDone = new HashMap<Long, Boolean>();
    for (Long id : candidateSprintIds) {
      hasDone.put(id, Boolean.FALSE);
    }
    for (Task t : tasks) {
      if (t.getSprintId() == null) {
        continue;
      }
      if (TaskStatus.DONE.equals(t.getStatus())) {
        hasDone.put(t.getSprintId(), Boolean.TRUE);
      }
    }
    for (Sprint sp : sprints) {
      if (!candidateSprintIds.contains(sp.getId())) {
        continue;
      }
      if (Boolean.TRUE.equals(hasDone.get(sp.getId()))) {
        continue;
      }
      String msg =
          "Sprint #"
              + sp.getId()
              + " 将在 "
              + sp.getEndDate()
              + " 结束，但当前无任何 DONE 状态的 task";
      out.add(new RiskFinding(RiskFinding.LEVEL_WARN, msg, "SPRINT", sp.getId(), name()));
    }
    return out;
  }
}
