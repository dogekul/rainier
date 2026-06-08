/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for {@link SprintController} DELETE. Covers TC-SPR-015..017. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private long[] seedReqOwnerProject() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long userId = userRepo.saveAndFlush(u).getId();
    Project p = new Project();
    p.setCode("PROJ-D");
    p.setName("x");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(userId);
    p.setEnabled(true);
    Long projectId = projectRepo.saveAndFlush(p).getId();
    Requirement r = new Requirement();
    r.setCode("REQ-D");
    r.setTitle("x");
    r.setOwnerUserId(userId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    Long reqId = requirementRepo.saveAndFlush(r).getId();
    return new long[] {userId, reqId};
  }

  private Long createSprintEntity(Long reqId, Long ownerId, String code) {
    Sprint sp = new Sprint();
    sp.setCode(code);
    sp.setName("x");
    sp.setStatus(SprintStatus.PLANNING);
    sp.setRequirementId(reqId);
    sp.setOwnerUserId(ownerId);
    return sprintRepo.saveAndFlush(sp).getId();
  }

  /** TC-SPR-015: 无引用软删 + GET 404. */
  @Test
  void delete_noRefs_returns204AndGet404() throws Exception {
    long[] seed = seedReqOwnerProject();
    Long sprintId = createSprintEntity(seed[1], seed[0], "SPR-NOREF");
    mockMvc.perform(delete("/api/sprints/" + sprintId)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/sprints/" + sprintId)).andExpect(status().isNotFound());
  }

  /** TC-SPR-016: 有 Story 引用 → 409 "sprint has linked stories". */
  @Test
  void delete_withStoryReference_returns409() throws Exception {
    long[] seed = seedReqOwnerProject();
    Long sprintId = createSprintEntity(seed[1], seed[0], "SPR-WITHSTORY");
    Story s = new Story();
    s.setCode("STR-FK");
    s.setTitle("x");
    s.setStatus(StoryStatus.DRAFT);
    s.setPriority(Priority.MEDIUM);
    s.setSprintId(sprintId);
    s.setOwnerUserId(seed[0]);
    storyRepo.saveAndFlush(s);
    mockMvc
        .perform(delete("/api/sprints/" + sprintId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("sprint has linked stories")));
  }

  /** TC-SPR-017: 多 Story 不同 status 仍 409. */
  @Test
  void delete_withMultipleStoryReferences_returns409() throws Exception {
    long[] seed = seedReqOwnerProject();
    Long sprintId = createSprintEntity(seed[1], seed[0], "SPR-MULTI");
    for (int i = 0; i < 3; i++) {
      Story s = new Story();
      s.setCode("STR-MULTI-" + i);
      s.setTitle("x");
      s.setStatus(i == 0 ? StoryStatus.DRAFT : i == 1 ? StoryStatus.IN_PROGRESS : StoryStatus.DONE);
      s.setPriority(Priority.MEDIUM);
      s.setSprintId(sprintId);
      s.setOwnerUserId(seed[0]);
      storyRepo.saveAndFlush(s);
    }
    mockMvc
        .perform(delete("/api/sprints/" + sprintId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("sprint has linked stories")));
  }
}
