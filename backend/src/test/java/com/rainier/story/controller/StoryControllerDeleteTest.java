/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link StoryController} DELETE. Covers TC-STR-016. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  /** TC-STR-016: DELETE 软删 + GET 404. */
  @Test
  void delete_softDeletes_returns204AndGet404() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long userId = userRepo.saveAndFlush(u).getId();
    Project p = new Project();
    p.setCode("PROJ-D");
    p.setName("X");
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
    Sprint sp = new Sprint();
    sp.setCode("SPR-D");
    sp.setName("x");
    sp.setStatus(SprintStatus.PLANNING);
    sp.setRequirementId(reqId);
    sp.setOwnerUserId(userId);
    Long sprintId = sprintRepo.saveAndFlush(sp).getId();

    ObjectNode body = json.createObjectNode();
    body.put("code", "STR-D1");
    body.put("title", "x");
    body.put("sprintId", sprintId);
    body.put("ownerUserId", userId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/stories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/api/stories/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/stories/" + id)).andExpect(status().isNotFound());
  }
}
