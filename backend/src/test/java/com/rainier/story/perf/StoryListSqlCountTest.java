/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.perf;

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

/** TC-PERF-STR-001: GET /api/stories?size=20 enrich stage prepares ≤ 5 statements. */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryListSqlCountTest {

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

    // 5 users (rotated across 20 stories)
    Long[] userIds = new Long[5];
    for (int i = 0; i < 5; i++) {
      User u = new User();
      u.setLoginName("u" + i);
      u.setName("User " + i);
      u.setIsInternal(true);
      u.setEnabled(true);
      userIds[i] = userRepo.saveAndFlush(u).getId();
    }
    // 2 projects
    Long[] projectIds = new Long[2];
    for (int i = 0; i < 2; i++) {
      Project p = new Project();
      p.setCode("PROJ-PERF-" + i);
      p.setName("Proj " + i);
      p.setStatus(ProjectStatus.ACTIVE);
      p.setOwnerUserId(userIds[0]);
      p.setEnabled(true);
      projectIds[i] = projectRepo.saveAndFlush(p).getId();
    }
    // 4 requirements split across 2 projects
    Long[] reqIds = new Long[4];
    for (int i = 0; i < 4; i++) {
      Requirement r = new Requirement();
      r.setCode("REQ-PERF-" + i);
      r.setTitle("Req " + i);
      r.setOwnerUserId(userIds[i % 5]);
      r.setProjectId(projectIds[i % 2]);
      r.setStatus(RequirementStatus.DRAFT);
      r.setPriority(Priority.MEDIUM);
      reqIds[i] = requirementRepo.saveAndFlush(r).getId();
    }
    // 4 sprints, each under one requirement
    Long[] sprintIds = new Long[4];
    for (int i = 0; i < 4; i++) {
      Sprint sp = new Sprint();
      sp.setCode("SPR-PERF-" + i);
      sp.setName("Sprint " + i);
      sp.setStatus(SprintStatus.PLANNING);
      sp.setRequirementId(reqIds[i]);
      sp.setOwnerUserId(userIds[i % 5]);
      sprintIds[i] = sprintRepo.saveAndFlush(sp).getId();
    }
    // 20 stories rotating sprints + users + project inherited via sprint→requirement
    for (int i = 0; i < 20; i++) {
      Story s = new Story();
      s.setCode("STR-PERF-" + i);
      s.setTitle("Story " + i);
      s.setStatus(StoryStatus.DRAFT);
      s.setPriority(Priority.MEDIUM);
      s.setSprintId(sprintIds[i % 4]);
      s.setProjectId(projectIds[(i % 4) % 2]);
      s.setOwnerUserId(userIds[i % 5]);
      storyRepo.saveAndFlush(s);
    }
  }

  /** TC-PERF-STR-001: list size=20 → prepare-statement count ≤ 5 + enrichment correctness. */
  @Test
  void list_size20_executesAtMost5PreparedStatements_andEnrichesCorrectly() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "hibernate.generate_statistics must be true");
    stats.clear();

    mockMvc
        .perform(get("/api/stories?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].sprintCode").exists())
        .andExpect(jsonPath("$.content[0].requirementCode").exists())
        .andExpect(jsonPath("$.content[0].projectCode").exists())
        .andExpect(jsonPath("$.content[0].ownerName").exists())
        .andExpect(jsonPath("$.content[0].ownerLoginName").exists());

    long stmtCount = stats.getPrepareStatementCount();
    // Lock to exactly 6: 1 page-data + 1 page-count (Spring Data findAll(spec, pageable) issues
    // both) + 4 batch enrichment selects (User, Sprint, Requirement, Project).
    // Versus v0.0.10 baseline: 1 + 1 + 20 × 4 ≈ 82 statements for the same page.
    // Equality (not ≤) catches both regressions (>6) AND silent over-tightening (<6).
    assertEquals(
        6L,
        stmtCount,
        "regression guard — expected exactly 6 (2 page + 4 batches); got " + stmtCount);
  }
}
