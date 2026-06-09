/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.domain.ProductCategoryStatus;
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

/** TC-PROD-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository repo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    categoryRepo.deleteAll();
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

  private Long createCategory(String code, String name, Long ownerUserId) {
    ProductCategory c = new ProductCategory();
    c.setCode(code);
    c.setName(name);
    c.setStatus(ProductCategoryStatus.ACTIVE);
    c.setOwnerUserId(ownerUserId);
    return categoryRepo.saveAndFlush(c).getId();
  }

  /** TC-PROD-001: minimal payload → 201, default status=PLANNING, categoryName + ownerName enrichment. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndEnrichment() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-FIN", "金融产品", uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-PAY");
    body.put("name", "支付平台");
    body.put("categoryId", cid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/products/\\d+")))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.categoryCode").value("CAT-FIN"))
        .andExpect(jsonPath("$.categoryName").value("金融产品"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"));
  }

  /** TC-PROD-002: categoryId not found → 400. */
  @Test
  void post_unknownCategory_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-X");
    body.put("name", "x");
    body.put("categoryId", 999_999L);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("category not found")));
  }

  /** TC-PROD-003: duplicate code → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-A", "x", uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-DUP");
    body.put("name", "x");
    body.put("categoryId", cid);
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
    Long cid = createCategory("CAT-Z", "x", uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-Z");
    body.put("name", "x");
    body.put("categoryId", cid);
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

  /** TC-PROD-005: missing required fields → 400 fieldErrors (name + categoryId + ownerUserId). */
  @Test
  void post_missingRequired_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-MISS");
    mockMvc
        .perform(
            post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("categoryId")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-PROD-006: createBy auto-injected. */
  @Test
  void post_createBy_autoInjected() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-CB", "x", uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-CB");
    body.put("name", "x");
    body.put("categoryId", cid);
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
