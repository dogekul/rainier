/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.43 AI work log create / list / decision. Covers TC-AIW-001..009. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiWorkLogControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AiWorkLogRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long seedLog(String agentType, String statusVal) {
    AiWorkLog a = new AiWorkLog();
    a.setAgentType(agentType);
    a.setAction("ACT");
    a.setSummary("s");
    a.setEvidence("evidence ref");
    a.setStatus(statusVal);
    return repo.saveAndFlush(a).getId();
  }

  /** TC-AIW-001: create proposal → 201, PROPOSED. */
  @Test
  void create_returns201Proposed() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("agentType", "STATUS_SYNC");
    body.put("action", "UPDATE_TASK_STATUS");
    body.put("summary", "task done");
    body.put("evidence", "commit abc123");
    mockMvc
        .perform(
            post("/api/ai-work-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PROPOSED"))
        .andExpect(jsonPath("$.evidence").value("commit abc123"));
  }

  /** TC-AIW-002: missing evidence → 400. */
  @Test
  void create_missingEvidence_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("agentType", "STATUS_SYNC");
    body.put("action", "X");
    body.put("summary", "s");
    mockMvc
        .perform(
            post("/api/ai-work-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-AIW-003: list filtered by status. */
  @Test
  void list_filterByStatus() throws Exception {
    seedLog("A", AiWorkLogStatus.PROPOSED);
    seedLog("B", AiWorkLogStatus.PROPOSED);
    seedLog("C", AiWorkLogStatus.ACCEPTED);
    mockMvc
        .perform(get("/api/ai-work-logs?status=PROPOSED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].status", everyItem(org.hamcrest.Matchers.is("PROPOSED"))));
  }

  /** TC-AIW-004: accept → 200, ACCEPTED, decidedBy set. */
  @Test
  void decide_accept() throws Exception {
    Long id = seedLog("A", AiWorkLogStatus.PROPOSED);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "ACCEPTED");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACCEPTED"))
        .andExpect(jsonPath("$.decidedBy").isNotEmpty());
  }

  /** TC-AIW-005: reject without reason → 400. */
  @Test
  void decide_rejectWithoutReason_returns400() throws Exception {
    Long id = seedLog("A", AiWorkLogStatus.PROPOSED);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECTED");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-AIW-006: reject with reason → 200, REJECTED + rejectReason. */
  @Test
  void decide_rejectWithReason() throws Exception {
    Long id = seedLog("A", AiWorkLogStatus.PROPOSED);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECTED");
    body.put("reason", "误判");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("REJECTED"))
        .andExpect(jsonPath("$.rejectReason").value("误判"));
  }

  /** TC-AIW-007: re-deciding a decided log → 409. */
  @Test
  void decide_alreadyDecided_returns409() throws Exception {
    Long id = seedLog("A", AiWorkLogStatus.ACCEPTED);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "REJECTED");
    body.put("reason", "x");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict());
  }

  /** TC-AIW-008: invalid decision → 400. */
  @Test
  void decide_invalidDecision_returns400() throws Exception {
    Long id = seedLog("A", AiWorkLogStatus.PROPOSED);
    ObjectNode body = json.createObjectNode();
    body.put("decision", "MAYBE");
    mockMvc
        .perform(
            post("/api/ai-work-logs/" + id + "/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-AIW-009: decide unknown id → 404. */
  @Test
  void decide_unknownId_returns404() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("decision", "ACCEPTED");
    mockMvc
        .perform(
            post("/api/ai-work-logs/999999/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isNotFound());
  }
}
