/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.controller;

import static org.hamcrest.Matchers.contains;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link DemandRequirementLinkController}. Covers TC-DRL-001..005. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemandRequirementLinkControllerTest {

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

  private Long createDemand(Long submitterId) {
    Demand d = new Demand();
    d.setTitle("x");
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

  /** TC-DRL-001: 合法链接创建。 */
  @Test
  void post_validLink_returns201() throws Exception {
    Long userId = createUser();
    Long demandId = createDemand(userId);
    Long reqId = createReq(userId, "REQ-LINK1");
    ObjectNode body = json.createObjectNode();
    body.put("demandId", demandId);
    body.put("requirementId", reqId);
    body.put("linkType", "RELATED");
    mockMvc
        .perform(
            post("/api/demand-requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.demandId").value(demandId))
        .andExpect(jsonPath("$.requirementId").value(reqId))
        .andExpect(jsonPath("$.linkType").value("RELATED"));
  }

  /** TC-DRL-002: 唯一性冲突 (demand_id, requirement_id)。 */
  @Test
  void post_duplicateLink_returns409() throws Exception {
    Long userId = createUser();
    Long demandId = createDemand(userId);
    Long reqId = createReq(userId, "REQ-LINK2");
    ObjectNode body = json.createObjectNode();
    body.put("demandId", demandId);
    body.put("requirementId", reqId);
    body.put("linkType", "DERIVED");
    mockMvc
        .perform(
            post("/api/demand-requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/demand-requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("link already exists")));
  }

  /** TC-DRL-003: demandId 不存在 → 400. */
  @Test
  void post_unknownDemandId_returns400() throws Exception {
    Long userId = createUser();
    Long reqId = createReq(userId, "REQ-LINK3");
    ObjectNode body = json.createObjectNode();
    body.put("demandId", 999_999L);
    body.put("requirementId", reqId);
    body.put("linkType", "DERIVED");
    mockMvc
        .perform(
            post("/api/demand-requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("demand not found")));
  }

  /** TC-DRL-004: 按 demandId 过滤列表。 */
  @Test
  void getList_filterByDemandId_returnsOnlyMatching() throws Exception {
    Long userId = createUser();
    Long demandId1 = createDemand(userId);
    Long demandId2 = createDemand(userId);
    Long reqId1 = createReq(userId, "REQ-LF1");
    Long reqId2 = createReq(userId, "REQ-LF2");
    Long reqId3 = createReq(userId, "REQ-LF3");
    postLink(demandId1, reqId1);
    postLink(demandId1, reqId2);
    postLink(demandId2, reqId3);
    mockMvc
        .perform(get("/api/demand-requirements?demandId=" + demandId1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        // Strong assertion: every returned row's demandId is the filter value (catches a filter
        // bug that would silently return rows for demandId2 too).
        .andExpect(
            jsonPath(
                "$.content[*].demandId", contains(demandId1.intValue(), demandId1.intValue())));
  }

  /** TC-DRL-005: 硬删。 */
  @Test
  void delete_returnsNoContentAndRowVanishes() throws Exception {
    Long userId = createUser();
    Long demandId = createDemand(userId);
    Long reqId = createReq(userId, "REQ-LDEL");
    Long linkId = postLink(demandId, reqId);
    mockMvc.perform(delete("/api/demand-requirements/" + linkId)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/demand-requirements/" + linkId)).andExpect(status().isNotFound());
    org.junit.jupiter.api.Assertions.assertEquals(0, linkRepo.count());
  }

  private Long postLink(Long demandId, Long reqId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("demandId", demandId);
    body.put("requirementId", reqId);
    body.put("linkType", "RELATED");
    MvcResult res =
        mockMvc
            .perform(
                post("/api/demand-requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }
}
