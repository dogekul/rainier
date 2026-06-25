/* (C) 2026 Rainier — internal use only. */
package com.rainier.weekly.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import com.rainier.weekly.domain.WeeklyDraft;
import com.rainier.weekly.domain.WeeklyDraftStatus;
import com.rainier.weekly.repository.WeeklyDraftRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Weekly draft service (v0.0.71). Template-rules version — assembles a markdown draft from the
 * caller's DONE / IN_PROGRESS tasks + stories within {@code [periodStart, periodEnd]}. No LLM, no
 * push (A8 will replace the template + add email).
 */
@Service
@Transactional(readOnly = true)
public class WeeklyDraftService {

  private final WeeklyDraftRepository draftRepo;
  private final TaskRepository taskRepo;
  private final StoryRepository storyRepo;

  public WeeklyDraftService(
      WeeklyDraftRepository draftRepo, TaskRepository taskRepo, StoryRepository storyRepo) {
    this.draftRepo = draftRepo;
    this.taskRepo = taskRepo;
    this.storyRepo = storyRepo;
  }

  @Transactional
  public WeeklyDraft generate(Long userId, LocalDate periodStart, LocalDate periodEnd) {
    if (userId == null) {
      throw new BadRequestException("userId is required");
    }
    if (periodStart == null || periodEnd == null) {
      throw new BadRequestException("periodStart / periodEnd are required");
    }
    if (periodEnd.isBefore(periodStart)) {
      throw new BadRequestException("periodEnd must be >= periodStart");
    }

    Instant startInstant = periodStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
    Instant endInstant =
        LocalDateTime.of(periodEnd, LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant();

    List<Task> doneTasks =
        taskRepo.findAll(taskSpec(userId, TaskStatus.DONE, startInstant, endInstant));
    List<Story> doneStories =
        storyRepo.findAll(storySpec(userId, StoryStatus.DONE, startInstant, endInstant));
    List<Task> ongoingTasks =
        taskRepo.findAll(taskSpec(userId, TaskStatus.IN_PROGRESS, null, null));
    List<Story> ongoingStories =
        storyRepo.findAll(storySpec(userId, StoryStatus.IN_PROGRESS, null, null));

    String md =
        buildMarkdown(
            periodStart, periodEnd, doneTasks, doneStories, ongoingTasks, ongoingStories);

    WeeklyDraft d = new WeeklyDraft();
    d.setUserId(userId);
    d.setPeriodStart(periodStart);
    d.setPeriodEnd(periodEnd);
    d.setContentMarkdown(md);
    d.setStatus(WeeklyDraftStatus.DRAFT);
    d.setCreatedAt(Instant.now());
    return draftRepo.saveAndFlush(d);
  }

  @Transactional
  public WeeklyDraft accept(Long id) {
    WeeklyDraft d =
        draftRepo
            .findById(id)
            .orElseThrow(() -> new NotFoundException("weekly draft not found: id=" + id));
    if (!WeeklyDraftStatus.DRAFT.equals(d.getStatus())) {
      throw new ConflictException("draft already decided: " + d.getStatus());
    }
    d.setStatus(WeeklyDraftStatus.ACCEPTED);
    d.setAcceptedAt(Instant.now());
    return draftRepo.saveAndFlush(d);
  }

  public Page<WeeklyDraft> list(Long userId, Pageable pageable) {
    return draftRepo.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable);
  }

  // ---- spec helpers ----

  private Specification<Task> taskSpec(
      Long userId, String status, Instant startInstant, Instant endInstant) {
    return (root, q, cb) -> {
      Predicate p = cb.equal(root.get("assigneeUserId"), userId);
      p = cb.and(p, cb.equal(root.get("status"), status));
      if (startInstant != null && endInstant != null) {
        p = cb.and(p, cb.between(root.get("updateTime"), startInstant, endInstant));
      }
      return p;
    };
  }

  private Specification<Story> storySpec(
      Long userId, String status, Instant startInstant, Instant endInstant) {
    return (root, q, cb) -> {
      Predicate p = cb.equal(root.get("ownerUserId"), userId);
      p = cb.and(p, cb.equal(root.get("status"), status));
      if (startInstant != null && endInstant != null) {
        p = cb.and(p, cb.between(root.get("updateTime"), startInstant, endInstant));
      }
      return p;
    };
  }

  // ---- markdown ----

  private String buildMarkdown(
      LocalDate start,
      LocalDate end,
      List<Task> doneTasks,
      List<Story> doneStories,
      List<Task> ongoingTasks,
      List<Story> ongoingStories) {
    StringBuilder sb = new StringBuilder(512);
    sb.append("# 本周完成 (").append(start).append(" ~ ").append(end).append(")\n");
    sb.append("## Task\n");
    appendTaskLines(sb, doneTasks);
    sb.append("## Story\n");
    appendStoryLines(sb, doneStories);
    sb.append("\n# 进行中\n");
    sb.append("## Task\n");
    appendTaskLines(sb, ongoingTasks);
    sb.append("## Story\n");
    appendStoryLines(sb, ongoingStories);
    return sb.toString();
  }

  private void appendTaskLines(StringBuilder sb, List<Task> tasks) {
    List<String> lines = new ArrayList<>(tasks.size());
    for (Task t : tasks) {
      lines.add("- [" + safe(t.getCode()) + "] " + safe(t.getTitle()));
    }
    appendOrEmpty(sb, lines);
  }

  private void appendStoryLines(StringBuilder sb, List<Story> stories) {
    List<String> lines = new ArrayList<>(stories.size());
    for (Story s : stories) {
      lines.add("- [" + safe(s.getCode()) + "] " + safe(s.getTitle()));
    }
    appendOrEmpty(sb, lines);
  }

  private void appendOrEmpty(StringBuilder sb, List<String> lines) {
    if (lines.isEmpty()) {
      sb.append("- 无\n");
      return;
    }
    for (String l : lines) {
      sb.append(l).append('\n');
    }
  }

  private static String safe(String s) {
    return s == null ? "" : s;
  }
}
