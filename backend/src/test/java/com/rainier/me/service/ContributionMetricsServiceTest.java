/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.me.dto.ProfileResponse;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.84 richer-contribution-metrics — service-level tests. */
@SpringBootTest
@ActiveProfiles("test")
class ContributionMetricsServiceTest {

  @Autowired private ContributionMetricsService service;
  @Autowired private TaskRepository taskRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void clean() {
    storyRepo.deleteAll();
    taskRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String login) {
    User u = new User();
    u.setLoginName(login);
    u.setName(login);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Task seedTask(String code, Long assignee, String status) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle("T " + code);
    t.setProjectId(1L);
    t.setStatus(status);
    t.setPriority("MEDIUM");
    t.setAssigneeUserId(assignee);
    return taskRepo.saveAndFlush(t);
  }

  private Story seedStory(String code, Long owner, String status) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("S " + code);
    s.setStatus(status);
    s.setPriority("MEDIUM");
    s.setSprintId(1L);
    s.setOwnerUserId(owner);
    return storyRepo.saveAndFlush(s);
  }

  @Test
  void byStatus_grouped() {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    seedTask("T-1", alice, TaskStatus.TODO);
    seedTask("T-2", alice, TaskStatus.IN_PROGRESS);
    seedTask("T-3", alice, TaskStatus.IN_PROGRESS);
    seedTask("T-4", alice, TaskStatus.DONE);
    seedTask("T-5", alice, TaskStatus.BLOCKED);
    seedTask("T-OTHER", bob, TaskStatus.DONE); // wrong user — must be excluded
    seedStory("S-1", alice, StoryStatus.READY);
    seedStory("S-2", alice, StoryStatus.DONE);

    ProfileResponse.Contribution c = service.computeFor(alice);

    assertThat(c.getTasksByStatus())
        .containsEntry(TaskStatus.TODO, 1L)
        .containsEntry(TaskStatus.IN_PROGRESS, 2L)
        .containsEntry(TaskStatus.DONE, 1L)
        .containsEntry(TaskStatus.BLOCKED, 1L)
        .containsEntry(TaskStatus.CANCELLED, 0L);
    assertThat(c.getStoriesByStatus())
        .containsEntry(StoryStatus.READY, 1L)
        .containsEntry(StoryStatus.DONE, 1L)
        .containsEntry(StoryStatus.DRAFT, 0L)
        .containsEntry(StoryStatus.CANCELLED, 0L);
  }

  @Test
  void thisWeek_counts() {
    Long alice = seedUser("alice");
    // Two tasks created during the current week — JPA's AuditingEntityListener writes createTime
    // to "now", which by definition falls in the current ISO week (Mon 00:00 UTC).
    seedTask("T-A", alice, TaskStatus.TODO);
    Task done = seedTask("T-B", alice, TaskStatus.DONE);

    // Bump the DONE task's updateTime explicitly to "now" so the DONE-this-week proxy fires;
    // saveAndFlush above already sets it but we re-save to make the intent obvious.
    done.setUpdateTime(Instant.now());
    taskRepo.saveAndFlush(done);

    ProfileResponse.Contribution c = service.computeFor(alice);

    assertThat(c.getTasksThisWeek()).isGreaterThanOrEqualTo(2L);
    assertThat(c.getTasksDoneThisWeek()).isGreaterThanOrEqualTo(1L);
  }

  @Test
  void weeklyTrend_fourWeeks() {
    Long alice = seedUser("alice");
    seedTask("T-A", alice, TaskStatus.DONE);

    ProfileResponse.Contribution c = service.computeFor(alice);
    List<ProfileResponse.WeekBucket> trend = c.getWeeklyTrend();

    assertThat(trend).hasSize(4);
    // Ascending: each bucket's ISO label should be lexicographically <= the next (works because
    // labels are "YYYY-Www" zero-padded — note: this assumes no week-based-year boundary inside
    // the 4-week window. Allow equality only when neighbours differ; we just check order is
    // monotonically non-decreasing within the window).
    for (int i = 1; i < trend.size(); i++) {
      assertThat(trend.get(i).getWeek())
          .as("week %d label vs previous", i)
          .isGreaterThan(trend.get(i - 1).getWeek());
    }
  }

  @Test
  void weekStartUtc_isMondayMidnight() {
    // 2026-06-25 is a Thursday → week start should be 2026-06-22 00:00 UTC.
    Instant thursday = Instant.parse("2026-06-25T14:30:00Z");
    Instant monday = ContributionMetricsService.weekStartUtc(thursday, 0);
    assertThat(monday).isEqualTo(Instant.parse("2026-06-22T00:00:00Z"));

    Instant lastMonday = ContributionMetricsService.weekStartUtc(thursday, 1);
    assertThat(lastMonday).isEqualTo(Instant.parse("2026-06-15T00:00:00Z"));
    assertThat(ChronoUnit.DAYS.between(lastMonday, monday)).isEqualTo(7);
  }

  @Test
  void degraded_nullUserId_returnsZeroes() {
    ProfileResponse.Contribution c = service.computeFor(null);
    assertThat(c.getTasksByStatus()).containsEntry(TaskStatus.DONE, 0L);
    assertThat(c.getWeeklyTrend()).hasSize(4);
    assertThat(c.getTasksThisWeek()).isZero();
  }
}
