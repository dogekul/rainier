/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainier.common.domain.Priority;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.domain.DemandStatus;
import com.rainier.demand.domain.Source;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.domain.LinkType;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for auxiliary association queries: GET /api/requirements/{id}/source-demands
 * (TC-DRL-006) and GET /api/demands/{id}/derived-requirements (TC-DRL-007).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuxiliaryQueriesTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private RequirementRepository reqRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
    demandRepo.deleteAll();
    reqRepo.deleteAll();
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

  private Long createDemand(Long submitterId, String title) {
    Demand d = new Demand();
    d.setTitle(title);
    d.setDescription("x");
    d.setSubmitterUserId(submitterId);
    d.setStatus(DemandStatus.PENDING);
    d.setPriority(Priority.MEDIUM);
    d.setSource(Source.WEB);
    return demandRepo.saveAndFlush(d).getId();
  }

  private Long createReq(Long ownerId, String code) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("x");
    r.setDescription("x");
    r.setOwnerUserId(ownerId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return reqRepo.saveAndFlush(r).getId();
  }

  private void link(Long demandId, Long reqId, String type) {
    DemandRequirementLink l = new DemandRequirementLink();
    l.setDemandId(demandId);
    l.setRequirementId(reqId);
    l.setLinkType(type);
    linkRepo.saveAndFlush(l);
  }

  /** TC-DRL-006: GET /api/requirements/{id}/source-demands 返回 2 个 + 富化字段 + linkType + linkId. */
  @Test
  void getRequirementSourceDemands_returnsEnrichedList() throws Exception {
    Long userId = createUser();
    Long d10 = createDemand(userId, "demand10");
    Long d20 = createDemand(userId, "demand20");
    Long r1 = createReq(userId, "REQ-AUX1");
    link(d10, r1, LinkType.DERIVED);
    link(d20, r1, LinkType.RELATED);

    mockMvc
        .perform(get("/api/requirements/" + r1 + "/source-demands"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].demandId").exists())
        .andExpect(jsonPath("$[0].title").exists())
        .andExpect(jsonPath("$[0].linkType").exists())
        .andExpect(jsonPath("$[0].linkId").exists());
  }

  /** TC-DRL-007: GET /api/demands/{id}/derived-requirements 返回 1 + linkType="DERIVED". */
  @Test
  void getDemandDerivedRequirements_returnsEnrichedList() throws Exception {
    Long userId = createUser();
    Long d10 = createDemand(userId, "d10");
    Long r1 = createReq(userId, "REQ-AUX2");
    link(d10, r1, LinkType.DERIVED);

    mockMvc
        .perform(get("/api/demands/" + d10 + "/derived-requirements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].requirementId").value(r1))
        .andExpect(jsonPath("$[0].linkType").value("DERIVED"));
  }
}
