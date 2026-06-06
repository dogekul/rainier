/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.project.repository.ProjectRepository;
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

/** Integration tests for {@link ProjectController} POST. Covers TC-PRJ-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
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

  /** TC-PRJ-001: 最小 payload + 默认值 + 富化. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndEnrichment() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-001");
    body.put("name", "采购系统改造");
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/projects/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.code").value("PROJ-001"))
        .andExpect(jsonPath("$.name").value("采购系统改造"))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.ownerUserId").value(userId))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"));
  }

  /** TC-PRJ-002: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-001");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-PRJ-003: 缺 ownerUserId → 400. */
  @Test
  void post_missingOwnerUserId_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-002");
    body.put("name", "X");
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-PRJ-004: ownerUserId 不存在 → 400. */
  @Test
  void post_unknownOwnerUserId_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-003");
    body.put("name", "X");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }

  /** TC-PRJ-005: 非法 status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-004");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    body.put("status", "UNKNOWN_STATUS");
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-PRJ-006: createBy 自动注入登录 username. */
  @Test
  void post_createBy_autoInjectedByAuditor() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-005");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        // Test profile uses an anonymous auditor; the field is filled by AuditorAwareImpl (non-null
        // in tests; "system" or similar). Verifying it's present is enough for the unit invariant.
        .andExpect(jsonPath("$.createBy").exists());
  }
}
