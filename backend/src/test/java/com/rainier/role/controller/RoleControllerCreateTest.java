/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration tests for {@link RoleController} POST. Covers TC-ROL-001..003. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RoleRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  /** TC-ROL-001: 最小 payload + 默认值。 */
  @Test
  void post_minimalPayload_returns201WithDefaults() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PMO");
    body.put("name", "PMO");
    mockMvc
        .perform(
            post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/roles/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value("PMO"))
        .andExpect(jsonPath("$.name").value("PMO"))
        .andExpect(jsonPath("$.enabled").value(true));
  }

  /** TC-ROL-002: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PMO");
    body.put("name", "PMO");
    mockMvc
        .perform(
            post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-ROL-003: 缺 name → 400. */
  @Test
  void post_missingName_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PMO");
    mockMvc
        .perform(
            post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")));
  }
}
