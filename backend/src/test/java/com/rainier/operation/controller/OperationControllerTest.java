/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.operation.domain.Operation;
import com.rainier.operation.domain.OperationStage;
import com.rainier.operation.repository.OperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.44 售后运营 pipeline. Covers TC-OPR-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OperationRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long seedOp(String stage, String statusVal) {
    Operation o = new Operation();
    o.setCustomerName("X 集团");
    o.setTitle("采购系统运维");
    o.setStage(stage);
    o.setStatus(statusVal);
    return repo.saveAndFlush(o).getId();
  }

  /** TC-OPR-001: minimal create → 201, MAINTENANCE/ACTIVE. */
  @Test
  void create_returns201() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("customerName", "X 集团");
    body.put("title", "采购系统运维");
    mockMvc
        .perform(post("/api/operations").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.stage").value("MAINTENANCE"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  /** TC-OPR-002: linear advance (MAINTENANCE → OPERATING). */
  @Test
  void advance_linear() throws Exception {
    Long id = seedOp(OperationStage.MAINTENANCE, Operation.ACTIVE);
    mockMvc
        .perform(post("/api/operations/" + id + "/advance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stage").value("OPERATING"));
  }

  /** TC-OPR-003: advancing from the last stage → CLOSED. */
  @Test
  void advance_lastStage_closes() throws Exception {
    Long id = seedOp(OperationStage.REPURCHASE, Operation.ACTIVE);
    mockMvc
        .perform(post("/api/operations/" + id + "/advance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CLOSED"));
  }

  /** TC-OPR-004: advancing a CLOSED op → 409. */
  @Test
  void advance_closed_returns409() throws Exception {
    Long id = seedOp(OperationStage.REPURCHASE, Operation.CLOSED);
    mockMvc
        .perform(post("/api/operations/" + id + "/advance"))
        .andExpect(status().isConflict());
  }
}
