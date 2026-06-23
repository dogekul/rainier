/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.opportunity.domain.ArtifactType;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityArtifact;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** v0.0.45 流转产出物门禁 + 列查 + Word 导出. Covers TC-OAR-001..009. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpportunityArtifactTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OpportunityRepository repo;
  @Autowired private OpportunityArtifactRepository artifactRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    artifactRepo.deleteAll();
    repo.deleteAll();
  }

  private Long seedOpp(String stage, String statusVal) {
    Opportunity o = new Opportunity();
    o.setCustomerName("X 集团");
    o.setTitle("采购系统");
    o.setStage(stage);
    o.setStatus(statusVal);
    return repo.saveAndFlush(o).getId();
  }

  private ObjectNode artifact(String title, String content) {
    return json.createObjectNode().put("title", title).put("content", content);
  }

  private void advance(Long id, ObjectNode body, int expectedStatus) throws Exception {
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().is(expectedStatus));
  }

  /** TC-OAR-001: 线索推进缺《商机调研报告》→ 400，且阶段不变（事务未推进）。 */
  @Test
  void leadAdvance_missingReport_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isBadRequest());
    assertEquals(OpportunityStage.LEAD, repo.findById(id).get().getStage());
  }

  /** TC-OAR-002: 线索带报告推进 → 200/OPPORTUNITY + 新建 RESEARCH_REPORT(stageFrom=LEAD). */
  @Test
  void leadAdvance_withReport_advancesAndCreatesArtifact() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.set("artifact", artifact("调研报告", "客户背景与需求分析…"));
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
    assertEquals(OpportunityStage.LEAD, arts.get(0).getStageFrom());
  }

  /** TC-OAR-003: 商机决策缺《决策评审纪要》→ 400，且阶段不变。 */
  @Test
  void opportunityPass_missingMinutes_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    advance(id, body, 400);
    assertEquals(OpportunityStage.OPPORTUNITY, repo.findById(id).get().getStage());
  }

  /** TC-OAR-010: 商机决策缺 decision 且缺纪要 → 400（decision 校验先于产出物门禁），阶段不变。 */
  @Test
  void opportunityAdvance_missingDecisionAndArtifact_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isBadRequest());
    assertEquals(OpportunityStage.OPPORTUNITY, repo.findById(id).get().getStage());
  }

  /** TC-OAR-011: 导出他商机的产出物 → 404 (no IDOR across opportunities). */
  @Test
  void export_crossOpportunity_returns404() throws Exception {
    Long owner = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.set("artifact", artifact("调研", "…"));
    advance(owner, body, 200); // creates an artifact on `owner`
    Long artifactId = artifactRepo.findByOpportunityIdOrderByIdDesc(owner).get(0).getId();
    Long other = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    mockMvc
        .perform(get("/api/opportunities/" + other + "/artifacts/" + artifactId + "/export"))
        .andExpect(status().isNotFound());
  }

  /** TC-OAR-004: 商机通过带纪要 → 200/POC + DECISION_MINUTES(decision=PASS). */
  @Test
  void opportunityPass_withMinutes_advancesAndRecordsDecision() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    body.set("artifact", artifact("评审纪要", "评审通过，进入 POC"));
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

  /** TC-OAR-005: 商机否决带纪要 → 200/LOST + DECISION_MINUTES(decision=REJECT). */
  @Test
  void opportunityReject_withMinutes_lostAndRecordsDecision() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    body.set("artifact", artifact("评审纪要", "评审否决：预算不足"));
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOST"));
    List<OpportunityArtifact> arts = artifactRepo.findByOpportunityIdOrderByIdDesc(id);
    assertEquals(1, arts.size());
    assertEquals(ArtifactType.DECISION_MINUTES, arts.get(0).getType());
    assertEquals("REJECT", arts.get(0).getDecision());
  }

  /** TC-OAR-006: 非门禁转换 (实施 SURVEY → REQUIREMENT) 无 artifact 照常推进. */
  @Test
  void nonGatedTransition_noArtifactNeeded() throws Exception {
    Long id = seedOpp(OpportunityStage.SURVEY, OpportunityStatus.WON);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("REQUIREMENT"));
    assertTrue(artifactRepo.findByOpportunityIdOrderByIdDesc(id).isEmpty());
  }

  private void postArtifact(Long id, String type, String title, String content, String link)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", type);
    body.put("title", title);
    if (content != null) {
      body.put("content", content);
    }
    if (link != null) {
      body.put("link", link);
    }
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/artifacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  /** TC-OAR-012: POC → 投标 缺产出物 → 400，阶段不变（独立提交门禁）。 */
  @Test
  void pocAdvance_missingArtifacts_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.POC, OpportunityStatus.OPEN);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isBadRequest());
    assertEquals(OpportunityStage.POC, repo.findById(id).get().getStage());
  }

  /** TC-OAR-013: POC 备齐 4 类产出物（独立提交）后 → 200/BIDDING。 */
  @Test
  void pocAdvance_withAllFourArtifacts_advances() throws Exception {
    Long id = seedOpp(OpportunityStage.POC, OpportunityStatus.OPEN);
    postArtifact(id, ArtifactType.PRESENTATION_MATERIAL, "讲解材料1", null, "https://x/ppt1");
    postArtifact(id, ArtifactType.PRESENTATION_MATERIAL, "讲解材料2", null, "https://x/ppt2");
    postArtifact(id, ArtifactType.CLIENT_REQUIREMENTS, "诉求清单", null, "https://x/req");
    postArtifact(id, ArtifactType.POC_SCORE, "得分表", "维度A 90分", null);
    // still missing 差距分析报告 → 400
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isBadRequest());
    postArtifact(id, ArtifactType.GAP_ANALYSIS, "差距分析", "差距：集成能力", null);
    mockMvc
        .perform(post("/api/opportunities/" + id + "/advance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("BIDDING"));
  }

  /** TC-OAR-014: POST 产出物校验 — 未知类型 400 / 既无正文也无链接 400 / 链接类正常建. */
  @Test
  void postArtifact_validation() throws Exception {
    Long id = seedOpp(OpportunityStage.POC, OpportunityStatus.OPEN);
    ObjectNode bad = json.createObjectNode();
    bad.put("type", "NOPE");
    bad.put("title", "x");
    bad.put("content", "y");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/artifacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bad.toString()))
        .andExpect(status().isBadRequest());
    ObjectNode empty = json.createObjectNode();
    empty.put("type", ArtifactType.PRESENTATION_MATERIAL);
    empty.put("title", "无内容");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/artifacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(empty.toString()))
        .andExpect(status().isBadRequest());
    postArtifact(id, ArtifactType.PRESENTATION_MATERIAL, "讲解", null, "https://x/ppt");
    assertEquals(1, artifactRepo.findByOpportunityIdOrderByIdDesc(id).size());
  }

  /** TC-OAR-015: 链接类材料无需标题 → 201；title 兜底为类型名。 */
  @Test
  void postArtifact_linkWithoutTitle_defaultsTitle() throws Exception {
    Long id = seedOpp(OpportunityStage.POC, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("type", ArtifactType.PRESENTATION_MATERIAL);
    body.put("link", "https://x/ppt");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/artifacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("讲解材料"))
        .andExpect(jsonPath("$.link").value("https://x/ppt"));
  }

  // ---- v0.0.46 投标/合同 门禁 (TC-CAR-001..009) ----

  /** POST a link-kind artifact with NO title (链接类无需标题). */
  private void postLink(Long id, String type, String link) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", type);
    body.put("link", link);
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/artifacts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  private void seedContractArtifacts(Long id) throws Exception {
    postLink(id, ArtifactType.BID_WINNING_NOTICE, "https://x/notice");
    postLink(id, ArtifactType.CONTRACT_DRAFT, "https://x/contract");
    postArtifact(id, ArtifactType.CONTRACT_REVIEW_MINUTES, "评审纪要", "评审通过，建议签约", null);
    postLink(id, ArtifactType.REVIEW_EMAIL_ARCHIVE, "https://x/email.eml");
    postLink(id, ArtifactType.SIGNED_CONTRACT, "https://x/signed.pdf");
  }

  private void advanceJson(Long id, String decision, int expected) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("decision", decision);
    advance(id, body, expected);
  }

  /** TC-CAR-001: 投标 PASS 缺《投标文件》→ 400，阶段不变。 */
  @Test
  void biddingPass_missingBidDoc_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    advanceJson(id, "PASS", 400);
    assertEquals(OpportunityStage.BIDDING, repo.findById(id).get().getStage());
  }

  /** TC-CAR-002: 投标提交投标文件后 PASS → 200/CONTRACT。 */
  @Test
  void biddingPass_withBidDoc_advances() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    postLink(id, ArtifactType.BID_DOCUMENT, "https://x/bid");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("CONTRACT"));
  }

  /** TC-CAR-003: 投标文件可多份（无标题）→ 全落库 + 推进。 */
  @Test
  void biddingPass_multipleBidDocs_advances() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    postLink(id, ArtifactType.BID_DOCUMENT, "https://x/bid1");
    postLink(id, ArtifactType.BID_DOCUMENT, "https://x/bid2");
    assertEquals(2, artifactRepo.findByOpportunityIdOrderByIdDesc(id).size());
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("CONTRACT"));
  }

  /** TC-CAR-004: 合同 PASS 缺件 → 400 列出缺失《类型名》，阶段不变。 */
  @Test
  void contractPass_missingArtifacts_returns400ListsMissing() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.OPEN);
    postLink(id, ArtifactType.BID_WINNING_NOTICE, "https://x/notice");
    postLink(id, ArtifactType.CONTRACT_DRAFT, "https://x/contract");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("评审会议纪要")))
        .andExpect(jsonPath("$.message", containsString("邮件归档")))
        .andExpect(jsonPath("$.message", containsString("已盖章合同")));
    assertEquals(OpportunityStage.CONTRACT, repo.findById(id).get().getStage());
  }

  /** TC-CAR-005: 合同五件齐 PASS → 200/INITIATION + status=WON。 */
  @Test
  void contractPass_allArtifacts_wonAndInitiation() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.OPEN);
    seedContractArtifacts(id);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("INITIATION"))
        .andExpect(jsonPath("$.status").value("WON"));
  }

  /** TC-CAR-006: 投标 REJECT 无产出物 → 200/LOST（门禁仅 PASS 强制）。 */
  @Test
  void biddingReject_noArtifacts_lost() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOST"));
  }

  /** TC-CAR-007: 合同 REJECT 无产出物 → 200/LOST。 */
  @Test
  void contractReject_noArtifacts_lost() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("LOST"));
  }

  /** TC-CAR-008: 投标 PASS 带内联 artifact → 仍 400（链接类不内联创建），无产出物落库。 */
  @Test
  void biddingPass_inlineArtifact_stillRequiresBidDoc() throws Exception {
    Long id = seedOpp(OpportunityStage.BIDDING, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    body.set("artifact", artifact("投标", "正文内容"));
    advance(id, body, 400);
    assertEquals(OpportunityStage.BIDDING, repo.findById(id).get().getStage());
    assertTrue(artifactRepo.findByOpportunityIdOrderByIdDesc(id).isEmpty());
  }

  /** TC-CAR-009: 新类型注册 — label + kind（5 链接类 + 评审会议纪要报告类）。 */
  @Test
  void newArtifactTypes_labelsAndKinds() {
    assertTrue(ArtifactType.ALL.contains(ArtifactType.BID_DOCUMENT));
    assertTrue(ArtifactType.ALL.contains(ArtifactType.SIGNED_CONTRACT));
    assertEquals("投标文件", ArtifactType.label(ArtifactType.BID_DOCUMENT));
    assertEquals("中标公示", ArtifactType.label(ArtifactType.BID_WINNING_NOTICE));
    assertEquals("已盖章合同", ArtifactType.label(ArtifactType.SIGNED_CONTRACT));
    assertTrue(ArtifactType.isLink(ArtifactType.BID_DOCUMENT));
    assertTrue(ArtifactType.isLink(ArtifactType.BID_WINNING_NOTICE));
    assertTrue(ArtifactType.isLink(ArtifactType.CONTRACT_DRAFT));
    assertTrue(ArtifactType.isLink(ArtifactType.REVIEW_EMAIL_ARCHIVE));
    assertTrue(ArtifactType.isLink(ArtifactType.SIGNED_CONTRACT));
    assertFalse(ArtifactType.isLink(ArtifactType.CONTRACT_REVIEW_MINUTES));
  }

  /** TC-CAR-010: 合同缺《中标公示》《合同》→ 400 且列出该两类（钉死其必需性，对称于 TC-CAR-004）。 */
  @Test
  void contractPass_missingNoticeAndDraft_returns400ListsThem() throws Exception {
    Long id = seedOpp(OpportunityStage.CONTRACT, OpportunityStatus.OPEN);
    postArtifact(id, ArtifactType.CONTRACT_REVIEW_MINUTES, "评审纪要", "评审通过", null);
    postLink(id, ArtifactType.REVIEW_EMAIL_ARCHIVE, "https://x/email.eml");
    postLink(id, ArtifactType.SIGNED_CONTRACT, "https://x/signed.pdf");
    ObjectNode body = json.createObjectNode();
    body.put("decision", "PASS");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/advance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("中标公示")))
        .andExpect(jsonPath("$.message", containsString("《合同》")));
    assertEquals(OpportunityStage.CONTRACT, repo.findById(id).get().getStage());
  }

  /** TC-CAR-011: 商机否决缺《决策评审纪要》→ 400（requiredOnReject(OPPORTUNITY)=true 的强制方向）。 */
  @Test
  void opportunityReject_missingMinutes_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.OPPORTUNITY, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECT");
    advance(id, body, 400);
    assertEquals(OpportunityStage.OPPORTUNITY, repo.findById(id).get().getStage());
  }

  /** TC-OAR-007: 产出物标题/正文空 → 400 (no advance). */
  @Test
  void blankArtifact_returns400() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.set("artifact", artifact("   ", ""));
    advance(id, body, 400);
    assertEquals(OpportunityStage.LEAD, repo.findById(id).get().getStage());
  }

  /** TC-OAR-008: list returns artifacts newest-first; append-only (no write endpoint). */
  @Test
  void list_returnsArtifactsNewestFirst() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode report = json.createObjectNode();
    report.set("artifact", artifact("调研", "…"));
    advance(id, report, 200); // LEAD → OPPORTUNITY (RESEARCH_REPORT)
    ObjectNode minutes = json.createObjectNode();
    minutes.put("decision", "PASS");
    minutes.set("artifact", artifact("评审", "…"));
    advance(id, minutes, 200); // OPPORTUNITY → POC (DECISION_MINUTES)
    mockMvc
        .perform(get("/api/opportunities/" + id + "/artifacts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].type").value(ArtifactType.DECISION_MINUTES))
        .andExpect(jsonPath("$[1].type").value(ArtifactType.RESEARCH_REPORT));
  }

  /** TC-OAR-009: export returns a valid .docx (PK zip header + OOXML content-type). */
  @Test
  void export_returnsValidDocx() throws Exception {
    Long id = seedOpp(OpportunityStage.LEAD, OpportunityStatus.OPEN);
    ObjectNode body = json.createObjectNode();
    body.set("artifact", artifact("调研报告", "正文第一行\n正文第二行"));
    advance(id, body, 200);
    Long artifactId = artifactRepo.findByOpportunityIdOrderByIdDesc(id).get(0).getId();
    MvcResult res =
        mockMvc
            .perform(get("/api/opportunities/" + id + "/artifacts/" + artifactId + "/export"))
            .andExpect(status().isOk())
            .andReturn();
    String contentType = res.getResponse().getContentType();
    assertTrue(
        contentType != null && contentType.contains("wordprocessingml"),
        "content-type should be a Word document: " + contentType);
    byte[] bytes = res.getResponse().getContentAsByteArray();
    assertTrue(bytes.length > 0, "docx body must be non-empty");
    // .docx is a ZIP/OOXML container — first two bytes are 'P','K'.
    assertEquals('P', bytes[0]);
    assertEquals('K', bytes[1]);
  }
}
