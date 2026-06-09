/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.perf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import javax.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-PERF-SPR-001: GET /api/sprints?size=20 enrich stage prepares ≤ 6 statements (2 page + 3 batch
 * enrich + 1 storyCount aggregate). See pending-adjustments.md PA-1 for the 5 → 6 budget revision.
 */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintListSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();

    Long[] userIds = new Long[3];
    for (int i = 0; i < 3; i++) {
      User u = new User();
      u.setLoginName("u" + i);
      u.setName("User " + i);
      u.setIsInternal(true);
      u.setEnabled(true);
      userIds[i] = userRepo.saveAndFlush(u).getId();
    }
    Long[] projectIds = new Long[2];
    for (int i = 0; i < 2; i++) {
      Project p = new Project();
      p.setCode("PROJ-PERF-S-" + i);
      p.setName("Proj " + i);
      p.setStatus(ProjectStatus.ACTIVE);
      p.setOwnerUserId(userIds[0]);
      p.setEnabled(true);
      projectIds[i] = projectRepo.saveAndFlush(p).getId();
    }
    Long[] reqIds = new Long[5];
    for (int i = 0; i < 5; i++) {
      Requirement r = new Requirement();
      r.setCode("REQ-PERF-S-" + i);
      r.setTitle("Req " + i);
      r.setOwnerUserId(userIds[i % 3]);
      r.setProjectId(projectIds[i % 2]);
      r.setStatus(RequirementStatus.DRAFT);
      r.setPriority(Priority.MEDIUM);
      reqIds[i] = requirementRepo.saveAndFlush(r).getId();
    }
    // 20 sprints; each with 0-2 stories for storyCount aggregation coverage.
    for (int i = 0; i < 20; i++) {
      Sprint sp = new Sprint();
      sp.setCode("SPR-PERF-S-" + i);
      sp.setName("Sprint " + i);
      sp.setStatus(SprintStatus.PLANNING);
      sp.setRequirementId(reqIds[i % 5]);
      sp.setOwnerUserId(userIds[i % 3]);
      Long sprintId = sprintRepo.saveAndFlush(sp).getId();
      // Stories: sprint 0,1,2 get 2 each; 3-19 get 0.
      if (i < 3) {
        for (int k = 0; k < 2; k++) {
          Story s = new Story();
          s.setCode("STR-PERF-S-" + i + "-" + k);
          s.setTitle("Story " + i + "-" + k);
          s.setStatus(StoryStatus.DRAFT);
          s.setPriority(Priority.MEDIUM);
          s.setSprintId(sprintId);
          s.setProjectId(projectIds[i % 2]);
          s.setOwnerUserId(userIds[i % 3]);
          storyRepo.saveAndFlush(s);
        }
      }
    }
  }

  /** TC-PERF-SPR-001: list size=20 → prepare-statement count ≤ 6 + enrichment correctness. */
  @Test
  void list_size20_executesAtMost6PreparedStatements_andStoryCountMatches() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "hibernate.generate_statistics must be true");
    stats.clear();

    mockMvc
        .perform(get("/api/sprints?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].requirementCode").exists())
        .andExpect(jsonPath("$.content[0].projectCode").exists())
        .andExpect(jsonPath("$.content[0].ownerName").exists())
        .andExpect(jsonPath("$.content[0].storyCount").exists());

    long stmtCount = stats.getPrepareStatementCount();
    // Lock to exactly 6: 1 page-data + 1 page-count + 3 batch enrichment (user/req/project)
    // + 1 storyCount GROUP BY aggregate. Versus v0.0.10 baseline: 1 + 1 + 20 × 4 ≈ 82.
    assertEquals(
        6L,
        stmtCount,
        "regression guard — expected exactly 6 (2 page + 3 batches + 1 storyCount aggregate); got "
            + stmtCount);
  }
}
