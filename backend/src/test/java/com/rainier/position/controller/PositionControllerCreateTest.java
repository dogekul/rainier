/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.position.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for {@link PositionController} POST. Covers TC-POS-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PositionControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PositionRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  /** TC-POS-001: 最小 payload + 默认值。 */
  @Test
  void post_minimalPayload_returns201WithDefaults() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "BE_ENG");
    body.put("name", "Backend Engineer");
    body.put("category", "TECH");
    mockMvc
        .perform(
            post("/api/positions").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/positions/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value("BE_ENG"))
        .andExpect(jsonPath("$.name").value("Backend Engineer"))
        .andExpect(jsonPath("$.category").value("TECH"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  /** TC-POS-002: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "BE_ENG");
    body.put("name", "Backend Engineer");
    body.put("category", "TECH");
    mockMvc
        .perform(
            post("/api/positions").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/positions").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-POS-003: 非法 category → 400. */
  @Test
  void post_invalidCategory_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "BE_ENG");
    body.put("name", "Backend Engineer");
    body.put("category", "UNKNOWN_CAT");
    mockMvc
        .perform(
            post("/api/positions").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid category")));
  }

  /** TC-POS-004: 缺 name → 400. */
  @Test
  void post_missingName_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "BE_ENG");
    body.put("category", "TECH");
    mockMvc
        .perform(
            post("/api/positions").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")));
  }
}
