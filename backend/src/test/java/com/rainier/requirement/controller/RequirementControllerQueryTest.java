/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.requirement.domain.Requirement;
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

/** Integration tests for GET/PUT {@link RequirementController}. Covers TC-REQ-004..007. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementRepository repo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private com.rainier.opportunity.repository.OpportunityRepository oppRepo;
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

  private Long seedOpp() {
    com.rainier.opportunity.domain.Opportunity o = new com.rainier.opportunity.domain.Opportunity();
    o.setCustomerName("X 集团");
    o.setTitle("采购系统");
    o.setStage(com.rainier.opportunity.domain.OpportunityStage.REQUIREMENT);
    o.setStatus(com.rainier.opportunity.domain.OpportunityStatus.WON);
    return oppRepo.saveAndFlush(o).getId();
  }

  private void createReqWithOpp(Long ownerId, String code, Long oppId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "x");
    body.put("ownerUserId", ownerId);
    body.put("opportunityId", oppId);
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  /** TC-OGEN-R3: GET /requirements?opportunityId= 仅返回该商机派生的需求。 */
  @Test
  void list_filterByOpportunityId_returnsOnlyThatOpp() throws Exception {
    Long userId = createUser();
    Long oppA = seedOpp();
    Long oppB = seedOpp();
    createReqWithOpp(userId, "R-A1", oppA);
    createReqWithOpp(userId, "R-A2", oppA);
    createReqWithOpp(userId, "R-B1", oppB);
    mockMvc
        .perform(get("/api/requirements").param("opportunityId", String.valueOf(oppA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  /** TC-REQ-004: GET 详情完整字段集。 */
  @Test
  void get_existingId_returnsFullDetail() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-FULL");
    MvcResult res =
        mockMvc
            .perform(get("/api/requirements/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "title",
      "description",
      "ownerUserId",
      "status",
      "priority",
      "complexity",
      "projectId",
      "closeReason",
      "expectedDate",
      "createTime",
      "updateTime",
      "createBy",
      "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-REQ-005: 按 projectId 过滤（占位字段查询）。 */
  @Test
  void getList_filterByProjectId_returnsOnlyMatching() throws Exception {
    Long userId = createUser();
    Long id1 = createReq(userId, "REQ-A");
    createReq(userId, "REQ-B");
    // Set projectId=42 on REQ-A via the entity directly (no API to set projectId before update).
    Requirement r = repo.findById(id1).get();
    r.setProjectId(42L);
    repo.saveAndFlush(r);
    mockMvc
        .perform(get("/api/requirements?projectId=42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].projectId").value(42));
  }

  /** TC-REQ-006: PUT 状态更新。 v0.0.8: ownerUserId is now @NotNull on the DTO. */
  @Test
  void put_updateStatus_persists() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-U");
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-U");
    body.put("title", "x");
    body.put("description", "x");
    body.put("status", "IN_ANALYSIS");
    body.put("priority", "HIGH");
    body.put("ownerUserId", userId); // v0.0.8: now required
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_ANALYSIS"))
        .andExpect(jsonPath("$.priority").value("HIGH"));
  }

  /** TC-REQP-005: v0.0.8 — PUT 改 ownerUserId 转移负责人（v0.0.6 不可改语义反转）. */
  @Test
  void put_changeOwnerUserId_persists() throws Exception {
    Long userA = createUser();
    User userB = new User();
    userB.setLoginName("lili");
    userB.setName("黎立");
    userB.setIsInternal(true);
    userB.setEnabled(true);
    Long userBId = userRepo.saveAndFlush(userB).getId();

    Long id = createReq(userA, "REQ-OWN-V2");
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-OWN-V2");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", userBId);
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerUserId").value(userBId))
        // v0.0.8: owner enrichment SHALL follow the new owner (parallels TC-PRJ-009).
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }

  /** TC-REQP-006: PUT 新 ownerUserId 不存在 → 400. */
  @Test
  void put_unknownNewOwner_returns400() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-OWN-404");
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-OWN-404");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", org.hamcrest.Matchers.startsWith("owner user not found")));
  }

  /** TC-REQS-SPR-003 (v0.0.10): GET 详情 + list 项含 sprintCount = 该 Requirement 下非软删 Sprint 数。 */
  @Test
  void get_requirement_includesSprintCountEnrichment() throws Exception {
    Long userId = createUser();
    Long reqId = createReq(userId, "REQ-COUNT");
    // Seed 3 Sprints (PLANNING / ACTIVE / COMPLETED — all del_flag=0).
    String[] states = {SprintStatus.PLANNING, SprintStatus.ACTIVE, SprintStatus.COMPLETED};
    for (int i = 0; i < 3; i++) {
      Sprint sp = new Sprint();
      sp.setCode("SPR-CNT-" + i);
      sp.setName("x");
      sp.setStatus(states[i]);
      sp.setRequirementId(reqId);
      sp.setOwnerUserId(userId);
      sprintRepo.saveAndFlush(sp);
    }
    // GET single
    JsonNode singleBody =
        json.readTree(
            mockMvc
                .perform(get("/api/requirements/" + reqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sprintCount").value(3))
                .andReturn()
                .getResponse()
                .getContentAsString());
    // v0.0.10.1 Test-M5 fix: v0.0.10 dropped storyCount — guard against silent regression.
    org.junit.jupiter.api.Assertions.assertFalse(
        singleBody.has("storyCount"),
        "RequirementDetail must NOT contain storyCount field (v0.0.10 dropped it for sprintCount)");
    // GET list — find this Requirement and assert its sprintCount
    MvcResult res =
        mockMvc
            .perform(get("/api/requirements?page=0&size=20"))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode content = json.readTree(res.getResponse().getContentAsString()).get("content");
    boolean found = false;
    for (JsonNode item : content) {
      if (item.get("id").asLong() == reqId) {
        org.junit.jupiter.api.Assertions.assertEquals(3L, item.get("sprintCount").asLong());
        org.junit.jupiter.api.Assertions.assertFalse(
            item.has("storyCount"), "Requirement list item must NOT contain storyCount field");
        found = true;
      }
    }
    org.junit.jupiter.api.Assertions.assertTrue(found, "expected REQ-COUNT in list");
  }
}
