/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TC-REQE-001/002/004/005 — v0.0.19 requirement status / priority / expectedDate. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementEnrichTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long userId;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    userId = userRepo.saveAndFlush(u).getId();
  }

  private ObjectNode reqBody(String code) {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "x");
    body.put("ownerUserId", userId);
    return body;
  }

  private Long createReq(String code) throws Exception {
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reqBody(code).toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-REQE-001: 新状态创建成功. */
  @Test
  void post_newStatus_succeeds() throws Exception {
    ObjectNode body = reqBody("REQ-N1");
    body.put("status", "IN_ANALYSIS");
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("IN_ANALYSIS"));
  }

  /** TC-REQE-002: 所有旧状态值（remap 源）都被拒. */
  @Test
  void post_legacyStatuses_allReturn400() throws Exception {
    int i = 0;
    for (String legacy : new String[] {"IN_REVIEW", "APPROVED", "IN_DEV", "DEPRECATED"}) {
      ObjectNode body = reqBody("REQ-N2-" + (i++));
      body.put("status", legacy);
      mockMvc
          .perform(
              post("/api/requirements")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body.toString()))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message", startsWith("invalid status")));
    }
  }

  /** TC-REQE-004: 优先级 LOWEST 创建成功. */
  @Test
  void post_priorityLowest_succeeds() throws Exception {
    ObjectNode body = reqBody("REQ-N3");
    body.put("priority", "LOWEST");
    mockMvc
        .perform(
            post("/api/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.priority").value("LOWEST"));
  }

  /** TC-REQE-005: expectedDate create + update. */
  @Test
  void expectedDate_createThenUpdate() throws Exception {
    ObjectNode body = reqBody("REQ-N4");
    body.put("expectedDate", "2026-09-01");
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.expectedDate").value("2026-09-01"))
            .andReturn();
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    ObjectNode put = json.createObjectNode();
    put.put("code", "REQ-N4");
    put.put("title", "x");
    put.put("status", "DRAFT");
    put.put("priority", "MEDIUM");
    put.put("ownerUserId", userId);
    put.put("expectedDate", "2026-10-01");
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(put.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expectedDate").value("2026-10-01"));

    // PUT contract: expectedDate is full-replace (same as projectId) — omitting it clears the date.
    // The frontend always sends it, so a cleared input round-trips as a clear here.
    ObjectNode clear = json.createObjectNode();
    clear.put("code", "REQ-N4");
    clear.put("title", "x");
    clear.put("status", "DRAFT");
    clear.put("priority", "MEDIUM");
    clear.put("ownerUserId", userId);
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(clear.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.expectedDate").value(org.hamcrest.Matchers.nullValue()));
  }
}
