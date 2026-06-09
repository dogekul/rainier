/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.productcategory.repository.ProductCategoryRepository;
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

/** TC-PCAT-001..006 + 009 createBy. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCategoryControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductCategoryRepository repo;
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

  /** TC-PCAT-001: 最小 payload + 默认 status=ACTIVE + ownerName 富化. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndEnrichment() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-FIN");
    body.put("name", "金融产品");
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/product-categories/\\d+")))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"));
  }

  /** TC-PCAT-002: code 重复 → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-DUP");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-PCAT-003: ownerUserId 不存在 → 400. */
  @Test
  void post_unknownOwner_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-X");
    body.put("name", "x");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }

  /** TC-PCAT-004: 非法 status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-Z");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-PCAT-005: 缺必填字段 → 400 fieldErrors. */
  @Test
  void post_missingRequired_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-MISS");
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-PCAT-006: createBy 自动注入. */
  @Test
  void post_createBy_autoInjected() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-CB");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }
}
