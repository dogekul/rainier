/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.milestone.repository.MilestoneRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TC-MILE-001..013 — Milestone CRUD + validation + list filter/sort. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MilestoneControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private MilestoneRepository repo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private ObjectMapper json;

  private Long projectId;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    projectRepo.deleteAll();
    projectId = seedProject("PRJ-MILE-A");
  }

  private Long seedProject(String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName("X");
    p.setStatus("PLANNING");
    p.setOwnerUserId(1L);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private Long createMilestone(Long projId, String code, String statusVal, Integer sortOrder)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projId);
    body.put("code", code);
    body.put("name", "MS-" + code);
    body.put("targetDate", "2026-07-01");
    if (statusVal != null) {
      body.put("status", statusVal);
    }
    if (sortOrder != null) {
      body.put("sortOrder", sortOrder);
    }
    MvcResult res =
        mockMvc
            .perform(
                post("/api/milestones")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-MILE-001: 默认 PLANNED / sortOrder 0. */
  @Test
  void post_minimal_defaultsPlannedAndSortZero() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("code", "M-1");
    body.put("name", "需求评审完成");
    body.put("targetDate", "2026-07-01");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectId").value(projectId))
        .andExpect(jsonPath("$.code").value("M-1"))
        .andExpect(jsonPath("$.targetDate").value("2026-07-01"))
        .andExpect(jsonPath("$.status").value("PLANNED"))
        .andExpect(jsonPath("$.sortOrder").value(0));
  }

  /**
   * TC-MILE-002: 显式 status / sortOrder / actualDate (v0.0.87: legacy REACHED 输入 normalize 为
   * canonical DONE).
   */
  @Test
  void post_explicitFields_persisted() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("code", "M-2");
    body.put("name", "上线");
    body.put("targetDate", "2026-07-01");
    body.put("status", "REACHED");
    body.put("sortOrder", 5);
    body.put("actualDate", "2026-06-30");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.sortOrder").value(5))
        .andExpect(jsonPath("$.actualDate").value("2026-06-30"));
  }

  /** TC-MILE-003: projectId 不存在 → 400. */
  @Test
  void post_unknownProject_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", 999_999L);
    body.put("code", "M-3");
    body.put("name", "X");
    body.put("targetDate", "2026-07-01");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("project not found")));
  }

  /** TC-MILE-004: 缺 targetDate → 400. */
  @Test
  void post_missingTargetDate_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("code", "M-4");
    body.put("name", "X");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("targetDate")));
  }

  /** TC-MILE-014: 缺 code → 400 (code @NotBlank). */
  @Test
  void post_missingCode_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("name", "X");
    body.put("targetDate", "2026-07-01");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("code")));
  }

  /**
   * TC-MILE-005: 非法 status → 400 (v0.0.87: 旧用例 "DONE" 现在合法；改用真正未知的 "XXX" 触发校验)。
   */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("code", "M-5");
    body.put("name", "X");
    body.put("targetDate", "2026-07-01");
    body.put("status", "XXX");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-MILE-006: 同项目 code 重复 → 409. */
  @Test
  void post_duplicateCodeSameProject_returns409() throws Exception {
    createMilestone(projectId, "M-1", null, null);
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("code", "M-1");
    body.put("name", "X");
    body.put("targetDate", "2026-07-01");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-MILE-007: 跨项目同 code → 201. */
  @Test
  void post_sameCodeDifferentProject_returns201() throws Exception {
    createMilestone(projectId, "M-1", null, null);
    Long other = seedProject("PRJ-MILE-B");
    ObjectNode body = json.createObjectNode();
    body.put("projectId", other);
    body.put("code", "M-1");
    body.put("name", "X");
    body.put("targetDate", "2026-07-01");
    mockMvc
        .perform(
            post("/api/milestones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  /** TC-MILE-008: 按 projectId 过滤 + sortOrder 升序. */
  @Test
  void getList_filterByProjectIdSortedBySortOrder() throws Exception {
    createMilestone(projectId, "A", null, 2);
    createMilestone(projectId, "B", null, 1);
    Long other = seedProject("PRJ-MILE-C");
    createMilestone(other, "C", null, 0);
    MvcResult res =
        mockMvc
            .perform(get("/api/milestones?projectId=" + projectId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.content[*].projectId", everyItem(is(projectId.intValue()))))
            .andReturn();
    JsonNode content = json.readTree(res.getResponse().getContentAsString()).get("content");
    // B (sortOrder=1) before A (sortOrder=2).
    org.junit.jupiter.api.Assertions.assertEquals(1, content.get(0).get("sortOrder").asInt());
    org.junit.jupiter.api.Assertions.assertEquals("B", content.get(0).get("code").asText());
    org.junit.jupiter.api.Assertions.assertEquals(2, content.get(1).get("sortOrder").asInt());
  }

  /** TC-MILE-009: 按 status 过滤. */
  @Test
  void getList_filterByStatus() throws Exception {
    createMilestone(projectId, "P1", "PLANNED", 0);
    createMilestone(projectId, "P2", "PLANNED", 1);
    createMilestone(projectId, "R1", "REACHED", 2);
    mockMvc
        .perform(get("/api/milestones?projectId=" + projectId + "&status=PLANNED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("PLANNED"))));
  }

  /**
   * TC-MILE-010: 标记达成 (v0.0.87: 状态机要求 PLANNED → IN_PROGRESS → DONE，禁止 PLANNED 直跳 DONE)。
   */
  @Test
  void put_markReached_setsStatusAndActualDate() throws Exception {
    Long id = createMilestone(projectId, "M-1", "PLANNED", 0);
    // Step 1: PLANNED → IN_PROGRESS
    ObjectNode body1 = json.createObjectNode();
    body1.put("code", "M-1");
    body1.put("name", "MS-M-1");
    body1.put("targetDate", "2026-07-01");
    body1.put("status", "IN_PROGRESS");
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body1.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    // Step 2: IN_PROGRESS → DONE + actualDate
    ObjectNode body2 = json.createObjectNode();
    body2.put("code", "M-1");
    body2.put("name", "MS-M-1");
    body2.put("targetDate", "2026-07-01");
    body2.put("status", "DONE");
    body2.put("actualDate", "2026-07-02");
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body2.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.actualDate").value("2026-07-02"));
  }

  /** TC-MILE-011: 调整 sortOrder + targetDate. */
  @Test
  void put_adjustsSortOrderAndTargetDate() throws Exception {
    Long id = createMilestone(projectId, "M-1", null, 0);
    ObjectNode body = json.createObjectNode();
    body.put("code", "M-1");
    body.put("name", "MS-M-1");
    body.put("targetDate", "2026-08-01");
    body.put("status", "PLANNED");
    body.put("sortOrder", 9);
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sortOrder").value(9))
        .andExpect(jsonPath("$.targetDate").value("2026-08-01"));
  }

  /** TC-MILE-012: 更新非法 status → 400. */
  @Test
  void put_invalidStatus_returns400() throws Exception {
    Long id = createMilestone(projectId, "M-1", null, 0);
    ObjectNode body = json.createObjectNode();
    body.put("code", "M-1");
    body.put("name", "MS-M-1");
    body.put("targetDate", "2026-07-01");
    body.put("status", "XXX");
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-MILE-013: 软删 → 204 + 后续 404. */
  @Test
  void delete_softDeletes_thenGet404() throws Exception {
    Long id = createMilestone(projectId, "M-1", null, 0);
    mockMvc.perform(delete("/api/milestones/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/milestones/" + id)).andExpect(status().isNotFound());
  }
}
