/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

import com.rainier.milestone.domain.Milestone;
import com.rainier.milestone.repository.MilestoneRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rolls a set of project ids up into per-project health rows (v0.0.28) — open/overdue/blocked task
 * counts + overdue-milestone count + deterministic RYG, sorted worst-first. Pure aggregation over the
 * existing task/milestone tables; no AI. Reused by the cross-project cockpit, the team panel, and the
 * company/PMO/exec maps via {@link ScopeService}.
 */
@Service
@Transactional(readOnly = true)
public class PortfolioService {

  private static final Set<String> CLOSED_TASK = new HashSet<>();
  private static final String BLOCKED = "BLOCKED";
  private static final String MILESTONE_REACHED = "REACHED";

  static {
    CLOSED_TASK.add("DONE");
    CLOSED_TASK.add("CANCELLED");
  }

  private final ProjectRepository projectRepo;
  private final TaskRepository taskRepo;
  private final MilestoneRepository milestoneRepo;

  public PortfolioService(
      ProjectRepository projectRepo, TaskRepository taskRepo, MilestoneRepository milestoneRepo) {
    this.projectRepo = projectRepo;
    this.taskRepo = taskRepo;
    this.milestoneRepo = milestoneRepo;
  }

  public List<PortfolioRow> portfolio(Collection<Long> projectIds) {
    if (projectIds == null || projectIds.isEmpty()) {
      return new ArrayList<>();
    }
    LocalDate today = LocalDate.now();
    List<Task> tasks = taskRepo.findByProjectIdIn(projectIds);
    List<Milestone> milestones = milestoneRepo.findByProjectIdIn(projectIds);

    List<PortfolioRow> rows = new ArrayList<>();
    for (Project p : projectRepo.findAllById(projectIds)) {
      int open = 0;
      int overdue = 0;
      int blocked = 0;
      for (Task t : tasks) {
        if (!p.getId().equals(t.getProjectId()) || isClosed(t.getStatus())) {
          continue;
        }
        open++;
        if (BLOCKED.equals(t.getStatus())) {
          blocked++;
        }
        if (isOverdue(t.getDueDate(), today)) {
          overdue++;
        }
      }
      int overdueMilestones = 0;
      for (Milestone m : milestones) {
        if (p.getId().equals(m.getProjectId())
            && !MILESTONE_REACHED.equals(m.getStatus())
            && isOverdue(m.getTargetDate(), today)) {
          overdueMilestones++;
        }
      }
      rows.add(
          new PortfolioRow(
              p.getId(),
              p.getCode(),
              p.getName(),
              p.getStatus(),
              p.getOrganizationId(),
              open,
              overdue,
              blocked,
              overdueMilestones,
              Ryg.tier(open, overdue, blocked)));
    }
    rows.sort(
        (a, b) -> {
          int byRyg = Integer.compare(Ryg.order(a.getRyg()), Ryg.order(b.getRyg()));
          return byRyg != 0 ? byRyg : a.getProjectName().compareTo(b.getProjectName());
        });
    return rows;
  }

  private static boolean isClosed(String status) {
    return status != null && CLOSED_TASK.contains(status);
  }

  private static boolean isOverdue(LocalDate date, LocalDate today) {
    return date != null && date.isBefore(today);
  }
}
