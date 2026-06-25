/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.domain.StageActivity;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.opportunity.repository.StageActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** v0.0.90 D2 — 商机 stage 活动清单 + dashboard 整合 (TC-SA-001..007). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StageActivityControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OpportunityRepository oppRepo;
  @Autowired private StageActivityRepository repo;
  @Autowired private OpportunityArtifactRepository artifactRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    artifactRepo.deleteAll();
    oppRepo.deleteAll();
  }

  private Long seedOpp() {
    Opportunity o = new Opportunity();
    o.setCustomerName("Y 公司");
    o.setTitle("数据中台");
    o.setStage(OpportunityStage.POC);
    o.setStatus(OpportunityStatus.OPEN);
    return oppRepo.saveAndFlush(o).getId();
  }

  private Long addActivity(Long oppId, String stage, String title) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("activityTitle", title);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/opportunities/" + oppId + "/stages/" + stage + "/activities")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-SA-001: 不存在的 opportunityId → 404. */
  @Test
  void list_unknownOpportunity_returns404() throws Exception {
    mockMvc
        .perform(get("/api/opportunities/999999/stages/POC/activities"))
        .andExpect(status().isNotFound());
  }

  /** TC-SA-002: 无效 stageCode → 400. */
  @Test
  void list_unknownStage_returns400() throws Exception {
    Long id = seedOpp();
    mockMvc
        .perform(get("/api/opportunities/" + id + "/stages/UNKNOWN/activities"))
        .andExpect(status().isBadRequest());
  }

  /** TC-SA-003: add + list 顺序 (id asc) + 默认 status=PENDING. */
  @Test
  void add_thenList_returnsCreationOrder() throws Exception {
    Long id = seedOpp();
    addActivity(id, OpportunityStage.POC, "演示 PPT 准备");
    addActivity(id, OpportunityStage.POC, "客户预演");
    mockMvc
        .perform(get("/api/opportunities/" + id + "/stages/POC/activities"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].activityTitle").value("演示 PPT 准备"))
        .andExpect(jsonPath("$[0].status").value("PENDING"))
        .andExpect(jsonPath("$[1].activityTitle").value("客户预演"));
  }

  /** TC-SA-004: markDone → 状态=DONE 且 completedAt 非空. */
  @Test
  void markDone_setsCompletedAt() throws Exception {
    Long id = seedOpp();
    Long aid = addActivity(id, OpportunityStage.POC, "客户预演");
    mockMvc
        .perform(post("/api/stage-activities/" + aid + "/done"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.completedAt").exists());
  }

  /** TC-SA-005: skip → 状态=SKIPPED 且 completedAt 为空. */
  @Test
  void skip_doesNotSetCompletedAt() throws Exception {
    Long id = seedOpp();
    Long aid = addActivity(id, OpportunityStage.POC, "客户预演");
    mockMvc
        .perform(post("/api/stage-activities/" + aid + "/skip"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("SKIPPED"))
        .andExpect(jsonPath("$.completedAt").doesNotExist());
  }

  /** TC-SA-006: 已 DONE 再 markDone → 400 (终态不可改). */
  @Test
  void markDone_alreadyDone_returns400() throws Exception {
    Long id = seedOpp();
    Long aid = addActivity(id, OpportunityStage.POC, "X");
    mockMvc.perform(post("/api/stage-activities/" + aid + "/done")).andExpect(status().isOk());
    mockMvc
        .perform(post("/api/stage-activities/" + aid + "/done"))
        .andExpect(status().isBadRequest());
    assertEquals(StageActivity.STATUS_DONE, repo.findById(aid).get().getStatus());
  }

  /** TC-SA-007: dashboard 返回 activities + 该 stage 的 artifacts（依 stageFrom 过滤）. */
  @Test
  void dashboard_combinesActivitiesAndStageArtifacts() throws Exception {
    Long id = seedOpp();
    addActivity(id, OpportunityStage.POC, "活动1");
    // 直接在 repo 写一条 artifact，避免去走 advance gate
    com.rainier.opportunity.domain.OpportunityArtifact a =
        new com.rainier.opportunity.domain.OpportunityArtifact();
    a.setOpportunityId(id);
    a.setType(com.rainier.opportunity.domain.ArtifactType.PRESENTATION_MATERIAL);
    a.setStageFrom(OpportunityStage.POC);
    a.setTitle("讲解材料");
    a.setLink("https://x/ppt");
    artifactRepo.saveAndFlush(a);
    // 一条不属于 POC 的产出物（无 stageFrom）应被过滤
    com.rainier.opportunity.domain.OpportunityArtifact other =
        new com.rainier.opportunity.domain.OpportunityArtifact();
    other.setOpportunityId(id);
    other.setType(com.rainier.opportunity.domain.ArtifactType.RESEARCH_REPORT);
    other.setStageFrom(OpportunityStage.LEAD);
    other.setTitle("调研");
    other.setContent("…");
    artifactRepo.saveAndFlush(other);

    mockMvc
        .perform(get("/api/opportunities/" + id + "/stages/POC/dashboard"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stageCode").value("POC"))
        .andExpect(jsonPath("$.activities.length()").value(1))
        .andExpect(jsonPath("$.activities[0].activityTitle").value("活动1"))
        .andExpect(jsonPath("$.artifacts.length()").value(1))
        .andExpect(jsonPath("$.artifacts[0].type").value("PRESENTATION_MATERIAL"));
  }

  /** Edge: addActivity 空标题 → 400. */
  @Test
  void addActivity_blankTitle_returns400() throws Exception {
    Long id = seedOpp();
    ObjectNode body = json.createObjectNode();
    body.put("activityTitle", "   ");
    mockMvc
        .perform(
            post("/api/opportunities/" + id + "/stages/POC/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }
}
