/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.me.dto.ProfileResponse;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.84 richer-contribution-metrics — produces the {@link ProfileResponse.Contribution} block:
 * by-status histograms for tasks/stories, this-week task counts, and a 4-week DONE trend.
 *
 * <p>Time math is ISO-8601 in UTC: a "week" starts at Monday 00:00:00 UTC and is identified by
 * {@code YYYY-'W'ww} (ISO week-based year + week-of-week-based-year). All time fields read are
 * {@code createTime} / {@code updateTime} from {@code BaseEntity}.
 */
@Service
@Transactional(readOnly = true)
public class ContributionMetricsService {

  /** Number of weeks (including the current week) in {@code weeklyTrend}. */
  static final int TREND_WEEKS = 4;

  private final TaskRepository taskRepo;
  private final StoryRepository storyRepo;

  public ContributionMetricsService(TaskRepository taskRepo, StoryRepository storyRepo) {
    this.taskRepo = taskRepo;
    this.storyRepo = storyRepo;
  }

  public ProfileResponse.Contribution computeFor(Long userId) {
    return computeFor(userId, Instant.now());
  }

  /** Package-private overload — lets tests pin {@code now} for deterministic trend windows. */
  ProfileResponse.Contribution computeFor(Long userId, Instant now) {
    ProfileResponse.Contribution out = new ProfileResponse.Contribution();
    if (userId == null) {
      // Defensive: degraded callers (unknown subject) already short-circuit upstream, but keep
      // this branch null-safe so a test seam never NPEs.
      out.setTasksByStatus(emptyStatusMap(TaskStatus.ALL));
      out.setStoriesByStatus(emptyStatusMap(StoryStatus.ALL));
      out.setWeeklyTrend(emptyTrend(now));
      return out;
    }

    out.setTasksByStatus(tasksByStatus(userId));
    out.setStoriesByStatus(storiesByStatus(userId));

    Instant weekStart = weekStartUtc(now, 0);
    out.setTasksThisWeek(taskRepo.countByAssigneeUserIdAndCreateTimeGreaterThanEqual(userId, weekStart));
    out.setTasksDoneThisWeek(
        taskRepo.countByAssigneeUserIdAndStatusAndUpdateTimeGreaterThanEqual(
            userId, TaskStatus.DONE, weekStart));

    out.setWeeklyTrend(weeklyTrend(userId, now));
    return out;
  }

  private Map<String, Long> tasksByStatus(Long userId) {
    Map<String, Long> m = new LinkedHashMap<>();
    // Preserve a stable, declared order (mirrors TaskStatus order — TODO first, CANCELLED last).
    for (String s :
        Arrays.asList(
            TaskStatus.TODO,
            TaskStatus.IN_PROGRESS,
            TaskStatus.DONE,
            TaskStatus.BLOCKED,
            TaskStatus.CANCELLED)) {
      m.put(s, taskRepo.countByAssigneeUserIdAndStatus(userId, s));
    }
    return m;
  }

  private Map<String, Long> storiesByStatus(Long userId) {
    Map<String, Long> m = new LinkedHashMap<>();
    for (String s :
        Arrays.asList(
            StoryStatus.DRAFT,
            StoryStatus.READY,
            StoryStatus.IN_PROGRESS,
            StoryStatus.DONE,
            StoryStatus.BLOCKED,
            StoryStatus.CANCELLED)) {
      m.put(s, storyRepo.countByOwnerUserIdAndStatus(userId, s));
    }
    return m;
  }

  private List<ProfileResponse.WeekBucket> weeklyTrend(Long userId, Instant now) {
    List<ProfileResponse.WeekBucket> trend = new ArrayList<>(TREND_WEEKS);
    // Oldest first: index 0 = TREND_WEEKS-1 weeks back, last = current week.
    for (int i = TREND_WEEKS - 1; i >= 0; i--) {
      Instant from = weekStartUtc(now, i);
      Instant to = weekStartUtc(now, i - 1);
      String label = isoWeekLabel(from);
      long tasksDone =
          taskRepo.countByAssigneeUserIdAndStatusAndUpdateTimeBetween(
              userId, TaskStatus.DONE, from, to);
      long storiesDone =
          storyRepo.countByOwnerUserIdAndStatusAndUpdateTimeBetween(
              userId, StoryStatus.DONE, from, to);
      trend.add(new ProfileResponse.WeekBucket(label, tasksDone, storiesDone));
    }
    return trend;
  }

  private List<ProfileResponse.WeekBucket> emptyTrend(Instant now) {
    List<ProfileResponse.WeekBucket> trend = new ArrayList<>(TREND_WEEKS);
    for (int i = TREND_WEEKS - 1; i >= 0; i--) {
      Instant from = weekStartUtc(now, i);
      trend.add(new ProfileResponse.WeekBucket(isoWeekLabel(from), 0L, 0L));
    }
    return trend;
  }

  private static Map<String, Long> emptyStatusMap(java.util.Set<String> keys) {
    Map<String, Long> m = new LinkedHashMap<>();
    for (String k : keys) {
      m.put(k, 0L);
    }
    return m;
  }

  /**
   * Start of the ISO week containing {@code now}, minus {@code weeksBack} whole weeks. Week starts
   * Monday 00:00:00 UTC. Pure — package-private so tests can freeze time.
   */
  static Instant weekStartUtc(Instant now, int weeksBack) {
    LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);
    int delta = today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue(); // 0..6
    LocalDate monday = today.minusDays(delta + 7L * weeksBack);
    return monday.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  /** {@code YYYY-'W'ww} label for the ISO week containing {@code instant}. */
  static String isoWeekLabel(Instant instant) {
    LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    int weekYear = ldt.get(IsoFields.WEEK_BASED_YEAR);
    int week = ldt.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
    return String.format(Locale.ROOT, "%d-W%02d", weekYear, week);
  }

  // For tests — expose package-private helpers explicitly if needed.
  static String isoWeekLabelForTest(Instant instant) {
    return isoWeekLabel(instant);
  }
}
