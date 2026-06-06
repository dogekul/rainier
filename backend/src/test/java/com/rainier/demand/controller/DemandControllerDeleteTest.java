/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.domain.LinkType;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.requirement.repository.RequirementRepository;
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

/** Integration tests for {@link DemandController} DELETE. Covers TC-DMD-010..011. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemandControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DemandRepository demandRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
    demandRepo.deleteAll();
    requirementRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createDemand(Long submitterId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("title", "x");
    body.put("description", "x");
    body.put("submitterUserId", submitterId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/demands")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-DMD-010: 无关联软删成功。 */
  @Test
  void delete_noLinks_returns204AndGet404() throws Exception {
    Long userId = createUser();
    Long id = createDemand(userId);
    mockMvc.perform(delete("/api/demands/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/demands/" + id)).andExpect(status().isNotFound());
  }

  /** TC-DMD-011: 有关联软删被拒。 */
  @Test
  void delete_withLink_returns409() throws Exception {
    Long userId = createUser();
    Long demandId = createDemand(userId);
    // create a requirement via REST and link them directly via repo
    ObjectNode rBody = json.createObjectNode();
    rBody.put("code", "REQ-DMD-011");
    rBody.put("title", "x");
    rBody.put("description", "x");
    rBody.put("ownerUserId", userId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rBody.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    Long reqId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    DemandRequirementLink link = new DemandRequirementLink();
    link.setDemandId(demandId);
    link.setRequirementId(reqId);
    link.setLinkType(LinkType.DERIVED);
    linkRepo.saveAndFlush(link);
    mockMvc
        .perform(delete("/api/demands/" + demandId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("demand has linked requirements")));
  }
}
