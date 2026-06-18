/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

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
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.39 GET /api/me/pending-reviews. Covers TC-MEREV-001..005. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MePendingReviewsControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private SprintRepository sprintRepo;

  @BeforeEach
  void cleanDb() {
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
    sp.setRequirementId(1L); // plain Long column, no FK — fine for enrichment-by-id
    sp.setOwnerUserId(1L);
    return sprintRepo.saveAndFlush(sp).getId();
  }

  private void seedStory(
      String code,
      Long ownerId,
      Long projectId,
      Long sprintId,
      String priority,
      Long reviewerId,
      String reviewStatus) {
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

  /** TC-MEREV-001: only the caller's PENDING stories (not others', not APPROVED). */
  @Test
  void pendingReviews_returnsOnlyMyPending() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long sp = seedSprint("SP-1", "Sprint One");
    seedStory("S-A", bob, null, sp, Priority.MEDIUM, alice, ReviewStatus.PENDING); // mine, pending
    seedStory("S-B", alice, null, sp, Priority.MEDIUM, bob, ReviewStatus.PENDING); // bob's
    seedStory("S-C", bob, null, sp, Priority.MEDIUM, alice, ReviewStatus.APPROVED); // mine, decided
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/pending-reviews").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].code").value("S-A"))
        .andExpect(jsonPath("$[0].reviewStatus").value("PENDING"));
  }

  /** TC-MEREV-002: sorted priority-high-first (URGENT before LOW). */
  @Test
  void pendingReviews_sortedByPriority() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long sp = seedSprint("SP-2", "Sprint Two");
    seedStory("S-LOW", bob, null, sp, Priority.LOW, alice, ReviewStatus.PENDING);
    seedStory("S-URG", bob, null, sp, Priority.URGENT, alice, ReviewStatus.PENDING);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/pending-reviews").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].code").value("S-URG"))
        .andExpect(jsonPath("$[1].code").value("S-LOW"));
  }

  /** TC-MEREV-003: enrichment (ownerName / projectName / sprintName). */
  @Test
  void pendingReviews_enriched() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long owner = seedUser("carol", "Carol Owner");
    Long proj = seedProject("PX", owner);
    Long sp = seedSprint("SP-3", "Sprint Three");
    seedStory("S-E", owner, proj, sp, Priority.HIGH, alice, ReviewStatus.PENDING);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/pending-reviews").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].ownerName").value("Carol Owner"))
        .andExpect(jsonPath("$[0].projectName").value("Proj PX"))
        .andExpect(jsonPath("$[0].sprintName").value("Sprint Three"));
  }

  /** TC-MEREV-004: no token → 401. */
  @Test
  void pendingReviews_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/pending-reviews")).andExpect(status().isUnauthorized());
  }

  /** TC-MEREV-005: no pending reviews → empty array. */
  @Test
  void pendingReviews_none_returnsEmpty() throws Exception {
    seedUser("alice", "Alice");
    String token = authService.issueToken("alice");
    mockMvc
        .perform(get("/api/me/pending-reviews").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
