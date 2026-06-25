/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.common.domain.Priority;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.domain.DemandStatus;
import com.rainier.demand.domain.Source;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.domain.LinkType;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
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

/** v0.0.42 GET /api/me/inbox (PO 收件箱). Covers TC-INBOX-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeInboxControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private OpportunityRepository opportunityRepo;

  @BeforeEach
  void cleanDb() {
    // deleteAll soft-deletes Demand/Requirement (@SQLDelete); inbox queries honor @Where(del_flag=0)
    // so prior rows are excluded — isolation holds without the @Modifying hardDeleteAll (needs a tx).
    linkRepo.deleteAll();
    demandRepo.deleteAll();
    requirementRepo.deleteAll();
    opportunityRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedOpportunity(String title, Long productId) {
    Opportunity o = new Opportunity();
    o.setCustomerName("C");
    o.setTitle(title);
    o.setProductId(productId);
    return opportunityRepo.saveAndFlush(o).getId();
  }

  private Long seedRequirementWithOpp(
      String code, Long owner, String priority, Long projectId, Long oppId) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("req " + code);
    r.setDescription("x");
    r.setOwnerUserId(owner);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(priority);
    r.setProjectId(projectId);
    r.setOpportunityId(oppId);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedDemand(String title, String status, String priority, Long submitter) {
    Demand d = new Demand();
    d.setTitle(title);
    d.setDescription("x");
    d.setSubmitterUserId(submitter);
    d.setStatus(status);
    d.setPriority(priority);
    d.setSource(Source.WEB);
    return demandRepo.saveAndFlush(d).getId();
  }

  private Long seedRequirement(String code, Long owner, String priority, Long projectId) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("req " + code);
    r.setDescription("x");
    r.setOwnerUserId(owner);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(priority);
    r.setProjectId(projectId);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long seedProject(String code, String name, Long owner) {
    Project p = new Project();
    p.setCode(code);
    p.setName(name);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(owner);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private void link(Long demandId, Long requirementId) {
    DemandRequirementLink l = new DemandRequirementLink();
    l.setDemandId(demandId);
    l.setRequirementId(requirementId);
    l.setLinkType(LinkType.DERIVED);
    linkRepo.saveAndFlush(l);
  }

  /** TC-INBOX-001: unconverted demands = unlinked AND non-terminal. */
  @Test
  void inbox_unconvertedDemandsOnly() throws Exception {
    Long alice = seedUser("alice");
    seedDemand("D1-open-unlinked", DemandStatus.PENDING, Priority.MEDIUM, alice);
    Long d2 = seedDemand("D2-linked", DemandStatus.PENDING, Priority.MEDIUM, alice);
    seedDemand("D3-closed", DemandStatus.CLOSED, Priority.MEDIUM, alice);
    Long req = seedRequirement("REQ-X", alice, Priority.MEDIUM, null);
    link(d2, req);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/inbox").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unconvertedDemands", hasSize(1)))
        .andExpect(jsonPath("$.unconvertedDemands[0].title").value("D1-open-unlinked"));
  }

  /** TC-INBOX-002: my requirements filtered by owner. */
  @Test
  void inbox_myRequirementsByOwner() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    seedRequirement("R-ALICE", alice, Priority.MEDIUM, null);
    seedRequirement("R-BOB", bob, Priority.MEDIUM, null);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/inbox").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRequirements", hasSize(1)))
        .andExpect(jsonPath("$.myRequirements[0].code").value("R-ALICE"));
  }

  /** TC-INBOX-003: my requirements — projectName enrich + priority sort (URGENT first). */
  @Test
  void inbox_myRequirements_enrichAndSort() throws Exception {
    Long alice = seedUser("alice");
    Long proj = seedProject("P-1", "Apollo", alice);
    seedRequirement("R-LOW", alice, Priority.LOW, proj);
    seedRequirement("R-URG", alice, Priority.URGENT, null);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/inbox").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRequirements[*].code", contains("R-URG", "R-LOW")))
        .andExpect(jsonPath("$.myRequirements[?(@.code=='R-LOW')].projectName", contains("Apollo")));
  }

  /** TC-INBOX-004: unconverted demands sorted by priority (URGENT first). */
  @Test
  void inbox_unconvertedDemands_sortedByPriority() throws Exception {
    Long alice = seedUser("alice");
    seedDemand("D-LOW", DemandStatus.PENDING, Priority.LOW, alice);
    seedDemand("D-URG", DemandStatus.PENDING, Priority.URGENT, alice);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/inbox").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unconvertedDemands[*].title", contains("D-URG", "D-LOW")));
  }

  /** TC-INBOX-005: no token → 401. */
  @Test
  void inbox_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/inbox")).andExpect(status().isUnauthorized());
  }

  /** TC-INBOX-PROD-001 (v0.0.95): productId filter narrows requirements via opp.productId. */
  @Test
  void inbox_productIdFilter_keepsOnlyMatchingOppRequirements() throws Exception {
    Long alice = seedUser("alice");
    Long opp1 = seedOpportunity("Opp-P1", 100L);
    Long opp2 = seedOpportunity("Opp-P2", 200L);
    seedRequirementWithOpp("R-P1", alice, Priority.MEDIUM, null, opp1);
    seedRequirementWithOpp("R-P2", alice, Priority.MEDIUM, null, opp2);
    seedRequirement("R-NO-OPP", alice, Priority.MEDIUM, null);
    // an unlinked open demand — should be excluded when productId is provided
    seedDemand("D-unlinked", DemandStatus.PENDING, Priority.HIGH, alice);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/inbox?productId=100").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRequirements", hasSize(1)))
        .andExpect(jsonPath("$.myRequirements[0].code").value("R-P1"))
        .andExpect(jsonPath("$.unconvertedDemands", hasSize(0)));
  }

  /** TC-INBOX-PROD-002 (v0.0.95): no productId → original behaviour preserved. */
  @Test
  void inbox_noProductId_originalBehaviour() throws Exception {
    Long alice = seedUser("alice");
    Long opp1 = seedOpportunity("Opp-P1", 100L);
    seedRequirementWithOpp("R-P1", alice, Priority.MEDIUM, null, opp1);
    seedRequirement("R-NO-OPP", alice, Priority.MEDIUM, null);
    seedDemand("D-unlinked", DemandStatus.PENDING, Priority.HIGH, alice);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/inbox").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.myRequirements", hasSize(2)))
        .andExpect(jsonPath("$.unconvertedDemands", hasSize(1)));
  }

  /** TC-INBOX-006: token subject with no User → both sections empty. */
  @Test
  void inbox_unknownSubject_emptyBoth() throws Exception {
    seedUser("alice");
    seedDemand("D-open", DemandStatus.PENDING, Priority.MEDIUM, 1L); // exists but caller is ghost
    String token = authService.issueToken("ghost");

    mockMvc
        .perform(get("/api/me/inbox").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unconvertedDemands", hasSize(0)))
        .andExpect(jsonPath("$.myRequirements", hasSize(0)));
  }
}
