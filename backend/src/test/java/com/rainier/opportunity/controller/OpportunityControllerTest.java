/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.opportunity.bootstrap.OpportunityStageEnteredAtBackfill;
import com.rainier.opportunity.domain.ArtifactType;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityArtifact;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.customer.domain.Customer;
import com.rainier.customer.repository.CustomerRepository;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.repository.ProductRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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
  @Autowired private OpportunityArtifactRepository artifactRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private CustomerRepository customerRepo;
  @Autowired private ObjectMapper json;
  @Autowired private OpportunityStageEnteredAtBackfill stageEnteredAtBackfill;
  @PersistenceContext private EntityManager em;

  @BeforeEach
  void cleanDb() {
    artifactRepo.deleteAll();
    repo.deleteAll();
    projectRepo.deleteAll();
    productRepo.deleteAll();
    customerRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedProduct(String code) {
    Product p = new Product();
    p.setCode(code);
    p.setName(code + " 产品");
    p.setStatus("ACTIVE");
    p.setOwnerUserId(seedUser("owner-" + code));
    return productRepo.saveAndFlush(p).getId();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedProject(String code, String projectType) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(seedUser("owner-" + code));
    p.setEnabled(true);
    p.setProjectType(projectType);
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

  /** Seed an artifact directly (kind-aware: link types → link, else content). v0.0.46 gate prep. */
  private void seedArtifact(Long oppId, String type, String linkOrContent) {
    OpportunityArtifact a = new OpportunityArtifact();
    a.setOpportunityId(oppId);
    a.setType(type);
    a.setTitle(ArtifactType.label(type));
    if (ArtifactType.isLink(type)) {
      a.setLink(linkOrContent);
    } else {
      a.setContent(linkOrContent);
    }
    artifactRepo.saveAndFlush(a);
  }

  /** TC-OPP-001: minimal create → 201, LEAD/OPEN. */
  @Test
  void create_returns201LeadOpen() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X 集团");
    body.put("title", "采购系统");
    body.put("note", "首次接触，需求待澄清");
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stage").value("LEAD"))
        .andExpect(jsonPath("$.status").value("OPEN"))
        .andExpect(jsonPath("$.note").value("首次接触，需求待澄清"));
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

  /** TC-OPP-015: create with a 产品标签 → 201; productId persisted + productName enriched. */
  @Test
  void create_withProduct_linksAndEnriches() throws Exception {
    Long product = seedProduct("PRD-1");
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X 集团");
    body.put("title", "采购系统");
    body.put("productId", product);
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.productId").value(product))
        .andExpect(jsonPath("$.productName").value("PRD-1 产品"));
  }

  /** TC-OPP-016: create with an unknown 产品 → 400. */
  @Test
  void create_unknownProduct_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X");
    body.put("title", "y");
    body.put("productId", 999999L);
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-OPP-017: create选择已有客户 → links customerId + adopts its name. */
  @Test
  void create_withExistingCustomer_links() throws Exception {
    Customer c = new Customer();
    c.setName("中信集团");
    Long cid = customerRepo.saveAndFlush(c).getId();
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "随便写的名"); // ignored — entity name is authoritative
    body.put("title", "采购系统");
    body.put("customerId", cid);
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerId").value(cid))
        .andExpect(jsonPath("$.customerName").value("中信集团"));
  }

  /** TC-OPP-019: update edits note + links an existing customer (详情 再编辑). */
  @Test
  void update_editsNoteAndLinksCustomer() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    Customer c = new Customer();
    c.setName("华峰制造");
    Long cid = customerRepo.saveAndFlush(c).getId();
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "随便");
    body.put("title", "采购系统 V2");
    body.put("note", "补充：预算已确认");
    body.put("customerId", cid);
    mockMvc
        .perform(
            put("/api/opportunities/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("采购系统 V2"))
        .andExpect(jsonPath("$.note").value("补充：预算已确认"))
        .andExpect(jsonPath("$.customerId").value(cid))
        .andExpect(jsonPath("$.customerName").value("华峰制造"));
  }

  /** TC-OPP-018: create填新客户名(无 customerId) → 自动新建客户 + 链 customerId. */
  @Test
  void create_withNewCustomerName_autoCreates() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "新世界客户");
    body.put("title", "采购系统");
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customerName").value("新世界客户"))
        .andExpect(jsonPath("$.customerId").isNumber());
    assertEquals(1, customerRepo.findAll().size());
    assertEquals("新世界客户", customerRepo.findAll().get(0).getName());
  }

  /** TC-OSEA-01: create records stageEnteredAt (停留计时起点). */
  @Test
  void create_recordsStageEnteredAt() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X 集团");
    body.put("title", "采购系统");
    mockMvc
        .perform(post("/api/opportunities").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stageEnteredAt").isNotEmpty());
    List<Opportunity> all = repo.findAll();
    assertEquals(1, all.size());
    assertNotNull(all.get(0).getStageEnteredAt());
  }

  /** TC-OSEA-02: a stage-advancing PASS refreshes stageEnteredAt forward (计时归零). */
  @Test
  void advance_refreshesStageEnteredAt() throws Exception {
    Long id = seedOpp(OpportunityStage.SURVEY, OpportunityStatus.WON); // 非关口、无产出物门禁
    Instant t0 = Instant.now().minusSeconds(3600);
    Opportunity seeded = repo.findById(id).get();
    seeded.setStageEnteredAt(t0);
    repo.saveAndFlush(seeded);
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("REQUIREMENT"));
    em.clear();
    Opportunity after = repo.findById(id).get();
    assertNotNull(after.getStageEnteredAt());
    assertTrue(after.getStageEnteredAt().isAfter(t0), "advance should refresh stageEnteredAt forward");
  }

  /** TC-OSEA-03: a gate REJECT (stage unchanged → 丢单) does NOT refresh stageEnteredAt. */
  @Test
  void reject_doesNotRefreshStageEnteredAt() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    Instant t0 = Instant.now().minusSeconds(3600);
    Opportunity seeded = repo.findById(id).get();
    seeded.setStageEnteredAt(t0);
    repo.saveAndFlush(seeded);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOST"));
    em.clear();
    Opportunity after = repo.findById(id).get();
    // unchanged ≈ t0 (an hour ago); a refresh would land near now → assert it stays well in the past
    assertTrue(
        after.getStageEnteredAt().isBefore(Instant.now().minusSeconds(60)),
        "REJECT (no stage change) must not refresh stageEnteredAt");
  }

  /** TC-OSEA-04: backfill fills NULL stage_entered_at FROM update_time, only NULLs, without touching business fields. */
  @Test
  void backfill_fillsNullStageEnteredAtFromUpdateTime() {
    Long legacyId = seedOpp(OpportunityStage.POC, OpportunityStatus.OPEN); // service-bypassed → stage_entered_at NULL
    // a row that already has a value must NOT be touched (WHERE stage_entered_at IS NULL guard)
    Long presetId = seedOpp(OpportunityStage.SURVEY, OpportunityStatus.WON);
    Instant sentinel = Instant.parse("2020-01-01T00:00:00Z");
    Opportunity preset = repo.findById(presetId).get();
    preset.setStageEnteredAt(sentinel);
    repo.saveAndFlush(preset);
    em.clear();
    assertNull(repo.findById(legacyId).get().getStageEnteredAt(), "legacy precondition: NULL");
    Instant legacyUpd = repo.findById(legacyId).get().getUpdateTime();

    stageEnteredAtBackfill.run();
    em.clear();

    Opportunity legacy = repo.findById(legacyId).get();
    assertNotNull(legacy.getStageEnteredAt(), "backfill should fill the NULL");
    assertEquals(
        legacyUpd.toEpochMilli(),
        legacy.getStageEnteredAt().toEpochMilli(),
        "filled from update_time (not create_time / now)");
    assertEquals(OpportunityStage.POC, legacy.getStage()); // business field untouched
    assertEquals("X 集团", legacy.getCustomerName());
    // only-NULL guard: the already-populated row keeps its sentinel value
    assertEquals(
        sentinel.toEpochMilli(),
        repo.findById(presetId).get().getStageEnteredAt().toEpochMilli(),
        "WHERE stage_entered_at IS NULL — existing value must not be clobbered");
  }

  /** TC-OPP-003: non-gate advance (LEAD → OPPORTUNITY) — v0.0.45 requires 《商机调研报告》. */
  @Test
  void advance_nonGate() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.set("artifact", json.createObjectNode().put("title", "调研").put("content", "客户背景…"));
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("OPPORTUNITY"));
    List<OpportunityArtifact> arts = artifactRepo.findByOpportunityIdOrderByIdDesc(id);
    assertEquals(1, arts.size());
    assertEquals(ArtifactType.RESEARCH_REPORT, arts.get(0).getType());
  }

  /** TC-OPP-004: gate advance without decision → 400. */
  @Test
  void advance_gateNoDecision_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isBadRequest());
  }

  /** TC-OPP-005: gate PASS (OPPORTUNITY → POC) — v0.0.45 requires 《决策评审纪要》. */
  @Test
  void advance_gatePass() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    body.set("artifact", json.createObjectNode().put("title", "评审").put("content", "通过理由…"));
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("POC"));
    List<OpportunityArtifact> arts = artifactRepo.findByOpportunityIdOrderByIdDesc(id);
    assertEquals(1, arts.size());
    assertEquals(ArtifactType.DECISION_MINUTES, arts.get(0).getType());
    assertEquals("PASS", arts.get(0).getDecision());
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

  /** TC-OPP-007: CONTRACT PASS → WON + 进入实施 (stage=INITIATION 立项). v0.0.46: 需先备齐合同 5 件产出物。 */
  @Test
  void advance_contractPass_wonAndEntersDelivery() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.OPEN);
    seedArtifact(id, ArtifactType.BID_WINNING_NOTICE, "https://x/notice");
    seedArtifact(id, ArtifactType.CONTRACT_DRAFT, "https://x/contract");
    seedArtifact(id, ArtifactType.CONTRACT_REVIEW_MINUTES, "评审纪要");
    seedArtifact(id, ArtifactType.REVIEW_EMAIL_ARCHIVE, "https://x/email.eml");
    seedArtifact(id, ArtifactType.SIGNED_CONTRACT, "https://x/signed.pdf");
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

  /** TC-OPP-009 / TC-INI-01: initiate WON+PASS links an existing 对外-交付 project. */
  @Test
  void initiate_wonPass_linksExternalDeliveryProject() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    Long proj = seedProject("PRJ-1", ProjectType.EXTERNAL_DELIVERY);
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

  /** TC-INI-02: initiate linking a non-对外-交付 (CASUAL) project → 400. */
  @Test
  void initiate_linkNonDeliveryProject_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    Long proj = seedProject("PRJ-CASUAL", ProjectType.CASUAL);
    ObjectNode body = json.createObjectNode();
    body.put("projectId", proj);
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-INI-03: initiate inline-creates an 对外-交付 project and links it. */
  @Test
  void initiate_inlineCreatesExternalDeliveryProject() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    Long owner = seedUser("deliv-owner");
    ObjectNode body = json.createObjectNode();
    body.put("projectCode", "DLV-NEW-1");
    body.put("projectName", "交付项目一");
    body.put("projectOwnerUserId", owner);
    body.put("decision", "PASS");
    String resp =
        mockMvc
            .perform(
                post("/api/opportunities/" + id + "/initiate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long newProjId = json.readTree(resp).get("projectId").asLong();
    assertNotNull(newProjId);
    assertEquals(
        ProjectType.EXTERNAL_DELIVERY, projectRepo.findById(newProjId).get().getProjectType());
  }

  /** TC-INI-04: initiate with neither projectId nor create fields → 400. */
  @Test
  void initiate_noProjectInfo_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-INI-05: 内联新建但缺负责人（商机无 pm 且未传 projectOwnerUserId）→ 400（用户实测命中的分支）。 */
  @Test
  void initiate_inlineCreateMissingOwner_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON); // seedOpp 不设 pmUserId
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    body.put("projectCode", "DLV-NOOWNER");
    body.put("projectName", "缺负责人");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-INI-06: 同时提供 projectId 与新建字段 → 400（二选一守卫）。 */
  @Test
  void initiate_bothProjectIdAndCreate_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    Long proj = seedProject("PRJ-BOTH", ProjectType.EXTERNAL_DELIVERY);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    body.put("projectId", proj);
    body.put("projectCode", "DLV-DUP");
    body.put("projectName", "重复");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-INI-07: 关联不存在的 projectId → 400。 */
  @Test
  void initiate_unknownProjectId_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.WON);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    body.put("projectId", 999_999L);
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
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
