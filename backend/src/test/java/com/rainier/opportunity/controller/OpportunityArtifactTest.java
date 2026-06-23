/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
