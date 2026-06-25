/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
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

/** v0.0.82 task-review: pending-reviews 合并 Story + Task. Covers TC-TREV-006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MePendingReviewsTaskMergeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private SprintRepository sprintRepo;

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedProject(String code, Long ownerId) {
    Project p = new Project();
    p.setCode(code);
    p.setName("Proj " + code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private Long seedSprint(String code, String name) {
    Sprint sp = new Sprint();
    sp.setCode(code);
    sp.setName(name);
    sp.setStatus(SprintStatus.PLANNING);
    sp.setRequirementId(1L);
    sp.setOwnerUserId(1L);
    return sprintRepo.saveAndFlush(sp).getId();
  }

  private void seedStory(
      String code, Long ownerId, Long projectId, Long sprintId, String priority,
      Long reviewerId, String reviewStatus) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("Story " + code);
    s.setStatus(StoryStatus.READY);
    s.setPriority(priority);
    s.setSprintId(sprintId);
    s.setProjectId(projectId);
    s.setOwnerUserId(ownerId);
    s.setReviewerUserId(reviewerId);
    s.setReviewStatus(reviewStatus);
    storyRepo.saveAndFlush(s);
  }

  private void seedTask(
      String code, Long assigneeId, Long projectId, String priority,
      Long reviewerId, String reviewStatus) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle("Task " + code);
    t.setStatus(TaskStatus.TODO);
    t.setPriority(priority);
    t.setProjectId(projectId);
    t.setAssigneeUserId(assigneeId);
    t.setReviewerUserId(reviewerId);
    t.setReviewStatus(reviewStatus);
    taskRepo.saveAndFlush(t);
  }

  /** TC-TREV-006: pending-reviews returns Story + Task rows w/ kind tag. */
  @Test
  void pendingReviews_mergesStoryAndTask() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long proj = seedProject("PMRG", bob);
    Long sp = seedSprint("SP-MRG", "Sprint Merge");
    seedStory("S-1", bob, proj, sp, Priority.HIGH, alice, ReviewStatus.PENDING);
    seedTask("T-1", bob, proj, Priority.MEDIUM, alice, ReviewStatus.PENDING);
    seedTask("T-2", bob, proj, Priority.LOW, alice, ReviewStatus.PENDING);
    // noise — APPROVED task should NOT show; bob's task should NOT show
    seedTask("T-3", bob, proj, Priority.HIGH, alice, ReviewStatus.APPROVED);
    seedTask("T-4", alice, proj, Priority.HIGH, bob, ReviewStatus.PENDING);

    String token = authService.issueToken("alice");
    mockMvc
        .perform(get("/api/me/pending-reviews").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        // sorted: HIGH (S-1) → MEDIUM (T-1) → LOW (T-2)
        .andExpect(jsonPath("$[0].kind").value("STORY"))
        .andExpect(jsonPath("$[0].code").value("S-1"))
        .andExpect(jsonPath("$[0].storyId").exists())
        .andExpect(jsonPath("$[1].kind").value("TASK"))
        .andExpect(jsonPath("$[1].code").value("T-1"))
        .andExpect(jsonPath("$[1].taskId").exists())
        .andExpect(jsonPath("$[2].kind").value("TASK"))
        .andExpect(jsonPath("$[2].code").value("T-2"))
        // sanity: each row has reviewStatus PENDING
        .andExpect(
            jsonPath("$[*].reviewStatus",
                containsInAnyOrder("PENDING", "PENDING", "PENDING")));
  }
}
