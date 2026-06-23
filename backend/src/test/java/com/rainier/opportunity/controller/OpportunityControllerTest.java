/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
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

/** v0.0.44 售前商机 pipeline + gates + WON/LOST + 立项. Covers TC-OPP-001..011. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpportunityControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OpportunityRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedProject(String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(seedUser("owner-" + code));
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private Long seedOpp(String stage, String statusVal) {
    Opportunity o = new Opportunity();
    o.setCustomerName("X 集团");
    o.setTitle("采购系统");
    o.setStage(stage);
    o.setStatus(statusVal);
    return repo.saveAndFlush(o).getId();
  }

  /** TC-OPP-001: minimal create → 201, LEAD/OPEN. */
  @Test
  void create_returns201LeadOpen() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X 集团");
    body.put("title", "采购系统");
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stage").value("LEAD"))
        .andExpect(jsonPath("$.status").value("OPEN"));
  }

  /** TC-OPP-002: unknown owner → 400. */
  @Test
  void create_unknownOwner_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X");
    body.put("title", "y");
    body.put("pmUserId", 999999L);
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-OPP-003: non-gate advance (LEAD → OPPORTUNITY). */
  @Test
  void advance_nonGate() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("OPPORTUNITY"));
  }

  /** TC-OPP-004: gate advance without decision → 400. */
  @Test
  void advance_gateNoDecision_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isBadRequest());
  }

  /** TC-OPP-005: gate PASS (OPPORTUNITY → POC). */
  @Test
  void advance_gatePass() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("POC"));
  }

  /** TC-OPP-006: gate REJECT → LOST. */
  @Test
  void advance_gateReject_lost() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    body.put("note", "失标");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOST"));
  }

  /** TC-OPP-007: CONTRACT PASS → WON + 进入实施 (stage=INITIATION 立项). */
  @Test
  void advance_contractPass_wonAndEntersDelivery() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("WON"))
        .andExpect(jsonPath("$.stage").value("INITIATION"));
  }

  /** TC-OPP-008: advancing a LOST opp → 409. */
  @Test
  void advance_lost_returns409() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.LOST);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isConflict());
  }

  /** TC-OPP-012: a WON opp advances through 实施 (立项评审 PASS: INITIATION → 现场调研). */
  @Test
  void advance_delivery_initiationGatePass() throws Exception {
    Long id = seedOpp(OpportunityStage.INITIATION, OpportunityStatus.WON);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("SURVEY"))
        .andExpect(jsonPath("$.status").value("WON"));
  }

  /** TC-OPP-013: 验收 (ACCEPTANCE) is terminal → advancing → 409. */
  @Test
  void advance_acceptance_terminal_returns409() throws Exception {
    Long id = seedOpp(OpportunityStage.ACCEPTANCE, OpportunityStatus.WON);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isConflict());
  }

  /** TC-OPP-014: 立项评审 REJECT holds at 立项 (not LOST). */
  @Test
  void advance_initiationReject_holds() throws Exception {
    Long id = seedOpp(OpportunityStage.INITIATION, OpportunityStatus.WON);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("INITIATION"))
        .andExpect(jsonPath("$.status").value("WON"));
  }

  /** TC-OPP-009: initiate a WON opp with PASS links the projectId. */
  @Test
  void initiate_wonPass_linksProject() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    Long proj = seedProject("PRJ-1");
    ObjectNode body = json.createObjectNode();
    body.put("projectId", proj);
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectId").value(proj));
  }

  /** TC-OPP-010: initiate a non-WON opp → 409. */
  @Test
  void initiate_notWon_returns409() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("projectId", 1L);
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict());
  }

  /** TC-OPP-011: list filtered by status. */
  @Test
  void list_filterByStatus() throws Exception {
    seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    seedOpp(OpportunityStage.BIDDING, OpportunityStatus.LOST);
    mockMvc
        .perform(get("/api/opportunities?status=OPEN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[*].status", everyItem(org.hamcrest.Matchers.is("OPEN"))));
  }
}
