/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
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

/** Integration tests for {@link DemandController} POST. Covers TC-DMD-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemandControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private DemandRepository repo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  /** TC-DMD-001: 最小 payload + 默认值。 */
  @Test
  void post_minimalPayload_returns201WithDefaults() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("title", "采购系统反应慢");
    body.put("description", "下单要 30 秒才返回");
    body.put("submitterUserId", userId);
    mockMvc
        .perform(
            post("/api/demands").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/demands/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.title").value("采购系统反应慢"))
        .andExpect(jsonPath("$.submitterUserId").value(userId))
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.priority").value("MEDIUM"))
        .andExpect(jsonPath("$.source").value("WEB"))
        .andExpect(jsonPath("$.aiClassification").isEmpty())
        .andExpect(jsonPath("$.aiDuplicateHint").isEmpty());
  }

  /** TC-DMD-002: 缺 title → 400. */
  @Test
  void post_missingTitle_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("description", "x");
    body.put("submitterUserId", userId);
    mockMvc
        .perform(
            post("/api/demands").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("title")));
  }

  /** TC-DMD-003: submitter 不存在 → 400. */
  @Test
  void post_unknownSubmitter_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("title", "x");
    body.put("description", "x");
    body.put("submitterUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/demands").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("submitter user not found")));
  }

  /** TC-DMD-004: 非法 status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("title", "x");
    body.put("description", "x");
    body.put("submitterUserId", userId);
    body.put("status", "UNKNOWN_STATUS");
    mockMvc
        .perform(
            post("/api/demands").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }
}
