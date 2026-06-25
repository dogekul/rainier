/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.service;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * v0.0.87 (C7): Milestone status machine integration — exercise illegal transitions through both
 * PUT and the new POST /transition endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MilestoneServiceTransitionTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private MilestoneRepository repo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private ObjectMapper json;

  private Long projectId;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    projectRepo.deleteAll();
    Project p = new Project();
    p.setCode("PRJ-MSM-A");
    p.setName("X");
    p.setStatus("PLANNING");
    p.setOwnerUserId(1L);
    p.setEnabled(true);
    projectId = projectRepo.saveAndFlush(p).getId();
  }

  private Long createPlanned(String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("projectId", projectId);
    body.put("code", code);
    body.put("name", "MS-" + code);
    body.put("targetDate", "2026-07-01");
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

  private void putStatus(Long id, String status, int expectedStatus) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "M-1");
    body.put("name", "MS-M-1");
    body.put("targetDate", "2026-07-01");
    body.put("status", status);
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().is(expectedStatus));
  }

  /** PUT PLANNED → DONE 直跳 → 400 illegal transition. */
  @Test
  void put_planned_to_done_directly_rejected() throws Exception {
    Long id = createPlanned("M-1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "M-1");
    body.put("name", "MS-M-1");
    body.put("targetDate", "2026-07-01");
    body.put("status", "DONE");
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("illegal transition")));
  }

  /** PUT PLANNED → REACHED (legacy) normalize 为 DONE，仍是非法跳转 → 400. */
  @Test
  void put_planned_to_legacy_reached_rejected() throws Exception {
    Long id = createPlanned("M-1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "M-1");
    body.put("name", "MS-M-1");
    body.put("targetDate", "2026-07-01");
    body.put("status", "REACHED");
    mockMvc
        .perform(
            put("/api/milestones/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("illegal transition")));
  }

  /** PUT PLANNED → IN_PROGRESS → DONE 两步合法. */
  @Test
  void put_two_step_planned_to_done_ok() throws Exception {
    Long id = createPlanned("M-1");
    putStatus(id, "IN_PROGRESS", 200);
    putStatus(id, "DONE", 200);
  }

  /** POST /transition 非法 → 400. */
  @Test
  void postTransition_planned_to_done_rejected() throws Exception {
    Long id = createPlanned("M-1");
    ObjectNode body = json.createObjectNode();
    body.put("to", "DONE");
    mockMvc
        .perform(
            post("/api/milestones/" + id + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("illegal transition")));
  }

  /** POST /transition IN_PROGRESS → DONE 自动填 actualDate. */
  @Test
  void postTransition_inProgress_to_done_autoFillsActualDate() throws Exception {
    Long id = createPlanned("M-1");
    putStatus(id, "IN_PROGRESS", 200);
    ObjectNode body = json.createObjectNode();
    body.put("to", "DONE");
    body.put("reason", "smoke 通过");
    mockMvc
        .perform(
            post("/api/milestones/" + id + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"))
        .andExpect(jsonPath("$.actualDate").exists());
  }

  /** POST /transition 接受 legacy alias REACHED → normalize 后从 IN_PROGRESS → DONE 合法. */
  @Test
  void postTransition_acceptsLegacyReachedAlias() throws Exception {
    Long id = createPlanned("M-1");
    putStatus(id, "IN_PROGRESS", 200);
    ObjectNode body = json.createObjectNode();
    body.put("to", "REACHED");
    mockMvc
        .perform(
            post("/api/milestones/" + id + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DONE"));
  }

  /** POST /transition 未知 status → 400 invalid status. */
  @Test
  void postTransition_unknownStatus_rejected() throws Exception {
    Long id = createPlanned("M-1");
    ObjectNode body = json.createObjectNode();
    body.put("to", "XYZ");
    mockMvc
        .perform(
            post("/api/milestones/" + id + "/transition")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", containsString("invalid status")));
  }
}
