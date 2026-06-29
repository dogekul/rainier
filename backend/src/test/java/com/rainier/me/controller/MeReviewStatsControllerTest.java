/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.common.domain.Priority;
import com.rainier.story.domain.ReviewStatus;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.112 (H5) GET /api/me/review-stats. Covers TC-ARCHSTATS-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeReviewStatsControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private TaskRepository taskRepo;

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
    storyRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private void seedStory(String code, Long owner, Long reviewer, String reviewStatus) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("Story " + code);
    s.setStatus(StoryStatus.READY);
    s.setPriority(Priority.MEDIUM);
    s.setSprintId(99L);
    s.setOwnerUserId(owner);
    s.setReviewerUserId(reviewer);
    s.setReviewStatus(reviewStatus);
    storyRepo.saveAndFlush(s);
  }

  private void seedTask(String code, Long assignee, Long reviewer, String reviewStatus) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle("Task " + code);
    t.setStatus(TaskStatus.TODO);
    t.setPriority(Priority.MEDIUM);
    t.setSprintId(99L);
    t.setProjectId(123L); // NOT NULL column; FK is plain Long, no referential check
    t.setAssigneeUserId(assignee);
    t.setReviewerUserId(reviewer);
    t.setReviewStatus(reviewStatus);
    taskRepo.saveAndFlush(t);
  }

  /** TC-ARCHSTATS-001: pendingStoryCount only counts MY pending stories. */
  @Test
  void reviewStats_pendingStoryCount_isolatedByReviewer() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    seedStory("S-A1", bob, alice, ReviewStatus.PENDING);
    seedStory("S-A2", bob, alice, ReviewStatus.PENDING);
    seedStory("S-B1", alice, bob, ReviewStatus.PENDING);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/review-stats").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingStoryCount").value(2))
        .andExpect(jsonPath("$.pendingTaskCount").value(0));
  }

  /** TC-ARCHSTATS-002: pendingTaskCount is independent from Story count. */
  @Test
  void reviewStats_pendingTaskCount_separateFromStory() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    seedStory("S-X", bob, alice, ReviewStatus.PENDING);
    seedTask("T-1", bob, alice, ReviewStatus.PENDING);
    seedTask("T-2", bob, alice, ReviewStatus.PENDING);
    seedTask("T-3", bob, alice, ReviewStatus.PENDING);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/review-stats").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.pendingStoryCount").value(1))
        .andExpect(jsonPath("$.pendingTaskCount").value(3));
  }

  /**
   * TC-ARCHSTATS-003: approvedThisWeek / rejectedThisWeek count decided Story+Task rows whose
   * updateTime falls in the current ISO week (the proxy for reviewedAt).
   */
  @Test
  void reviewStats_thisWeekCounts() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    // 2 APPROVED stories + 1 REJECTED task — all just-created => updateTime is "now"
    seedStory("S-OK1", bob, alice, ReviewStatus.APPROVED);
    seedStory("S-OK2", bob, alice, ReviewStatus.APPROVED);
    seedTask("T-NO", bob, alice, ReviewStatus.REJECTED);
    // Sanity row for somebody else — must NOT be counted
    seedStory("S-Bob", alice, bob, ReviewStatus.APPROVED);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/review-stats").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.approvedThisWeek").value(2))
        .andExpect(jsonPath("$.rejectedThisWeek").value(1));
  }

  /** TC-ARCHSTATS-004: no token → 401. */
  @Test
  void reviewStats_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/review-stats")).andExpect(status().isUnauthorized());
  }
}
