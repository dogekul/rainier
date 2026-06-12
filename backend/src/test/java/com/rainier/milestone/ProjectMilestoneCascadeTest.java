/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainier.common.domain.Priority;
import com.rainier.milestone.domain.Milestone;
import com.rainier.milestone.domain.MilestoneStatus;
import com.rainier.milestone.repository.MilestoneRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TC-MILE-CAS-001/002 — project delete cascade soft-delete to milestones (entity-project MOD). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMilestoneCascadeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private MilestoneRepository milestoneRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    requirementRepo.deleteAll();
    milestoneRepo.deleteAll();
    projectRepo.deleteAll();
  }

  private Long seedProject(String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName("X");
    p.setStatus("PLANNING");
    p.setOwnerUserId(1L);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private void seedMilestone(Long projectId, String code) {
    Milestone m = new Milestone();
    m.setProjectId(projectId);
    m.setCode(code);
    m.setName("MS-" + code);
    m.setStatus(MilestoneStatus.PLANNED);
    m.setTargetDate(LocalDate.parse("2026-07-01"));
    m.setSortOrder(0);
    milestoneRepo.saveAndFlush(m);
  }

  /** Active (del_flag=0) milestone count for a project, via the API (respects @Where). */
  private long activeMilestoneCount(Long projectId) throws Exception {
    MvcResult res =
        mockMvc
            .perform(get("/api/milestones?projectId=" + projectId))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("total").asLong();
  }

  /** TC-MILE-CAS-001: deleting a project with milestones (no other refs) cascades soft-delete. */
  @Test
  void deleteProject_cascadeSoftDeletesMilestones() throws Exception {
    Long pid = seedProject("PRJ-CAS-1");
    seedMilestone(pid, "M-1");
    seedMilestone(pid, "M-2");
    assertEquals(2, activeMilestoneCount(pid), "two active milestones before delete");

    mockMvc.perform(delete("/api/projects/" + pid)).andExpect(status().isNoContent());

    assertEquals(0, activeMilestoneCount(pid), "milestones cascade soft-deleted with the project");
    // The project itself is gone too (guards against a cascade-without-project-delete bug).
    mockMvc.perform(get("/api/projects/" + pid)).andExpect(status().isNotFound());
  }

  /** TC-MILE-CAS-002: a project referenced by a Requirement still 409s; milestones untouched. */
  @Test
  void deleteProject_blockedByRequirement_milestonesUntouched() throws Exception {
    Long pid = seedProject("PRJ-CAS-2");
    seedMilestone(pid, "M-1");
    Requirement r = new Requirement();
    r.setCode("REQ-CAS");
    r.setTitle("x");
    r.setDescription("x");
    r.setOwnerUserId(1L);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    r.setProjectId(pid);
    requirementRepo.saveAndFlush(r);

    mockMvc
        .perform(delete("/api/projects/" + pid))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("project has linked requirements")));

    // Transaction rolled back — the milestone must remain active (cascade did not run).
    assertEquals(1, activeMilestoneCount(pid), "milestone untouched when delete is blocked");
  }
}
