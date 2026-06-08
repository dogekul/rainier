/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.domain.DemandStatus;
import com.rainier.demand.domain.Source;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.domain.LinkType;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
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

/** Integration tests for DELETE {@link RequirementController}. Covers TC-REQ-008..009. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementRepository repo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    linkRepo.deleteAll();
    demandRepo.deleteAll();
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createSprint(Long reqId, Long ownerId, String code) {
    Sprint sp = new Sprint();
    sp.setCode(code);
    sp.setName("x");
    sp.setStatus(SprintStatus.PLANNING);
    sp.setRequirementId(reqId);
    sp.setOwnerUserId(ownerId);
    return sprintRepo.saveAndFlush(sp).getId();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createReq(Long ownerId, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", ownerId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  private Long createDemandDirect(Long submitterId) {
    Demand d = new Demand();
    d.setTitle("x");
    d.setDescription("x");
    d.setSubmitterUserId(submitterId);
    d.setStatus(DemandStatus.PENDING);
    d.setPriority(Priority.MEDIUM);
    d.setSource(Source.WEB);
    return demandRepo.saveAndFlush(d).getId();
  }

  /** TC-REQ-008: 无关联软删 204 + 后续 GET 404. */
  @Test
  void delete_noLinks_returns204AndGet404() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-DEL");
    mockMvc.perform(delete("/api/requirements/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/requirements/" + id)).andExpect(status().isNotFound());
  }

  /** TC-REQ-009: 有关联软删被拒。 */
  @Test
  void delete_withLink_returns409() throws Exception {
    Long userId = createUser();
    Long reqId = createReq(userId, "REQ-LINK");
    Long demandId = createDemandDirect(userId);
    DemandRequirementLink link = new DemandRequirementLink();
    link.setDemandId(demandId);
    link.setRequirementId(reqId);
    link.setLinkType(LinkType.DERIVED);
    linkRepo.saveAndFlush(link);
    mockMvc
        .perform(delete("/api/requirements/" + reqId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("requirement has linked demands")));
  }

  /**
   * TC-REQS-SPR-002 (v0.0.10): when BOTH demand_requirement AND Sprint refs exist, the `requirement
   * has linked demands` check still fires first (intentional ordering).
   */
  @Test
  void delete_withDemandAndSprintReferences_returnsDemandsFirst() throws Exception {
    Long userId = createUser();
    Long reqId = createReq(userId, "REQ-BOTH");
    createSprint(reqId, userId, "SPR-BOTH");
    Long demandId = createDemandDirect(userId);
    DemandRequirementLink link = new DemandRequirementLink();
    link.setDemandId(demandId);
    link.setRequirementId(reqId);
    link.setLinkType(LinkType.DERIVED);
    linkRepo.saveAndFlush(link);
    mockMvc
        .perform(delete("/api/requirements/" + reqId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("requirement has linked demands")));
  }

  /** TC-REQS-SPR-001 (v0.0.10): Requirement 有 Sprint 引用 → 409 "requirement has linked sprints". */
  @Test
  void delete_withSprintReference_returns409() throws Exception {
    Long userId = createUser();
    Long reqId = createReq(userId, "REQ-WITH-SPRINT");
    createSprint(reqId, userId, "SPR-FK-1");
    mockMvc
        .perform(delete("/api/requirements/" + reqId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("requirement has linked sprints")));
  }
}
