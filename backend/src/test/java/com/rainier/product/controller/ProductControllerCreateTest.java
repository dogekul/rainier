/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.repository.ProductRepository;
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

/** TC-PROD-001..005 (v0.0.13 — categoryId removed from the Product contract). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  /** TC-PROD-001: minimal payload (无 categoryId) → 201, default PLANNING, owner enrich only. */
  @Test
  void post_minimalPayload_returns201WithoutCategoryFields() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-PAY");
    body.put("name", "支付平台");
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/products/\\d+")))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"))
        .andExpect(jsonPath("$.categoryId").doesNotExist())
        .andExpect(jsonPath("$.categoryCode").doesNotExist())
        .andExpect(jsonPath("$.categoryName").doesNotExist());
  }

  /** TC-PROD-002: 废弃 categoryId 字段静默忽略 → 201, 不存不返. */
  @Test
  void post_obsoleteCategoryIdField_silentlyIgnored() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-LEGACY");
    body.put("name", "x");
    body.put("categoryId", 999L); // dropped by Jackson — field no longer exists on the DTO
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.categoryId").doesNotExist());
  }

  /** TC-PROD-003: duplicate code → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-DUP");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-PROD-004: invalid status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-Z");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-PROD-005: missing required → 400 fieldErrors 含 name/ownerUserId, 不含 categoryId. */
  @Test
  void post_missingRequired_returns400WithoutCategoryFieldError() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-MISS");
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(
            jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")))
        .andExpect(
            jsonPath("$.fieldErrors[*].field", not(org.hamcrest.Matchers.hasItem("categoryId"))));
  }

  /** createBy auto-injected (v0.0.12 沿用). */
  @Test
  void post_createBy_autoInjected() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-CB");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }
}
