/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.domain.DemandStatus;
import com.rainier.demand.domain.Source;
import com.rainier.demand.repository.DemandRepository;
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

/**
 * Integration tests for workflow-demand-conversion: atomic create requirement + derived links.
 * Covers TC-DRC-001..003.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementConversionTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementRepository reqRepo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
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

  /** TC-DRC-001: 含 sourceDemandIds=[d1, d2] 成功转化 — 2 link 落库；后续 GET source-demands 长度=2. */
  @Test
  void post_withSourceDemandIds_createsRequirementAndLinks() throws Exception {
    Long userId = createUser();
    Long d1 = createDemand(userId);
    Long d2 = createDemand(userId);
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-CONV-1");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    ArrayNode ids = body.putArray("sourceDemandIds");
    ids.add(d1);
    ids.add(d2);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            // RequirementDetail must NOT include sourceDemandIds
            .andExpect(jsonPath("$.sourceDemandIds").doesNotExist())
            .andReturn();
    Long reqId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    org.junit.jupiter.api.Assertions.assertEquals(2, linkRepo.countByRequirementId(reqId));
    mockMvc
        .perform(get("/api/requirements/" + reqId + "/source-demands"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  /** TC-DRC-002: sourceDemandIds 中含 unknown id → 整体回滚 (req + links 都不存在). */
  @Test
  void post_withUnknownDemand_rollsBackEverything() throws Exception {
    Long userId = createUser();
    Long d1 = createDemand(userId);
    long preLinkCount = linkRepo.count();
    long preReqCount = reqRepo.count();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-CONV-ROLLBACK");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    ArrayNode ids = body.putArray("sourceDemandIds");
    ids.add(d1);
    ids.add(999_999L);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("demand not found")));
    // Rollback verification — neither the requirement nor any link should have persisted
    org.junit.jupiter.api.Assertions.assertEquals(preReqCount, reqRepo.count());
    org.junit.jupiter.api.Assertions.assertEquals(preLinkCount, linkRepo.count());
    org.junit.jupiter.api.Assertions.assertFalse(reqRepo.existsByCode("REQ-CONV-ROLLBACK"));
  }

  /** TC-DRC-003: 空数组或缺字段 → 普通创建（无 link 落库）。 */
  @Test
  void post_emptySourceDemandIds_createsRequirementOnly() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-CONV-EMPTY");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userId);
    body.putArray("sourceDemandIds"); // empty array
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    org.junit.jupiter.api.Assertions.assertEquals(0, linkRepo.count());
  }
}
