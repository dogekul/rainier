/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** Integration tests for {@link DemandController} GET/PUT. Covers TC-DMD-005..009. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemandControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DemandRepository repo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private com.rainier.opportunity.repository.OpportunityRepository oppRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
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

  private Long createDemand(Long submitterId, String title) throws Exception {
    return createDemand(submitterId, title, "PENDING");
  }

  private Long createDemand(Long submitterId, String title, String status) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("title", title);
    body.put("description", "x");
    body.put("submitterUserId", submitterId);
    body.put("status", status);
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

  private Long seedOpp() {
    com.rainier.opportunity.domain.Opportunity o = new com.rainier.opportunity.domain.Opportunity();
    o.setCustomerName("X 集团");
    o.setTitle("采购系统");
    o.setStage(com.rainier.opportunity.domain.OpportunityStage.REQUIREMENT);
    o.setStatus(com.rainier.opportunity.domain.OpportunityStatus.WON);
    return oppRepo.saveAndFlush(o).getId();
  }

  private void createDemandWithOpp(Long submitterId, String title, Long oppId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("title", title);
    body.put("description", "x");
    body.put("submitterUserId", submitterId);
    body.put("opportunityId", oppId);
    mockMvc
        .perform(
            post("/api/demands").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
  }

  /** TC-OGEN-D4: GET /demands?opportunityId= 仅返回该商机派生的诉求。 */
  @Test
  void list_filterByOpportunityId_returnsOnlyThatOpp() throws Exception {
    Long userId = createUser();
    Long oppA = seedOpp();
    Long oppB = seedOpp();
    createDemandWithOpp(userId, "A1", oppA);
    createDemandWithOpp(userId, "A2", oppA);
    createDemandWithOpp(userId, "B1", oppB);
    mockMvc
        .perform(get("/api/demands").param("opportunityId", String.valueOf(oppA)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2));
  }

  /** TC-DMD-005: GET 详情返完整字段集。 */
  @Test
  void get_existingId_returnsFullDetail() throws Exception {
    Long userId = createUser();
    Long id = createDemand(userId, "采购系统反应慢");
    MvcResult res =
        mockMvc
            .perform(get("/api/demands/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    // Verify the full field set
    String[] expected = {
      "id",
      "title",
      "description",
      "submitterUserId",
      "status",
      "priority",
      "source",
      "aiClassification",
      "aiDuplicateHint",
      "closeReason",
      "createTime",
      "updateTime",
      "createBy",
      "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-DMD-006: 软删后 GET 返 404. */
  @Test
  void get_softDeleted_returns404() throws Exception {
    Long userId = createUser();
    Long id = createDemand(userId, "x");
    mockMvc.perform(delete("/api/demands/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/demands/" + id)).andExpect(status().isNotFound());
  }

  /** TC-DMD-007: 按 status 过滤列表。 */
  @Test
  void getList_filterByStatus_returnsOnlyMatching() throws Exception {
    Long userId = createUser();
    createDemand(userId, "p1", "PENDING");
    createDemand(userId, "p2", "PENDING");
    createDemand(userId, "r1", "IN_REVIEW");
    mockMvc
        .perform(get("/api/demands?status=PENDING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].status", contains("PENDING", "PENDING")));
  }

  /** TC-DMD-008: PUT 更新 status/priority. */
  @Test
  void put_updateStatusAndPriority_persists() throws Exception {
    Long userId = createUser();
    Long id = createDemand(userId, "x");
    ObjectNode body = json.createObjectNode();
    body.put("title", "x");
    body.put("description", "x");
    body.put("status", "IN_REVIEW");
    body.put("priority", "HIGH");
    body.put("source", "WEB");
    mockMvc
        .perform(
            put("/api/demands/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_REVIEW"))
        .andExpect(jsonPath("$.priority").value("HIGH"));
  }

  /** TC-DMD-009: PUT 含 aiClassification 静默忽略（Jackson 忽略未知 + service 不读）。 */
  @Test
  void put_withAiClassificationInBody_silentlyIgnored() throws Exception {
    Long userId = createUser();
    Long id = createDemand(userId, "x");
    ObjectNode body = json.createObjectNode();
    body.put("title", "x");
    body.put("description", "x");
    body.put("aiClassification", "hack");
    mockMvc
        .perform(
            put("/api/demands/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        // aiClassification on the entity remains null (service does not accept it from PUT body)
        .andExpect(jsonPath("$.aiClassification").isEmpty());
  }
}
