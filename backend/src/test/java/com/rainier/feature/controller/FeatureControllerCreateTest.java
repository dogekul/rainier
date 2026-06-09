/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.domain.ProductCategoryStatus;
import com.rainier.productcategory.repository.ProductCategoryRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.domain.ProductModuleStatus;
import com.rainier.productmodule.repository.ProductModuleRepository;
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

/** TC-FEAT-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureRepository repo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
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

  private Long createCategory(String code, Long ownerUserId) {
    ProductCategory c = new ProductCategory();
    c.setCode(code);
    c.setName(code);
    c.setStatus(ProductCategoryStatus.ACTIVE);
    c.setOwnerUserId(ownerUserId);
    return categoryRepo.saveAndFlush(c).getId();
  }

  private Long createProduct(String code, Long categoryId, Long ownerUserId) {
    Product p = new Product();
    p.setCode(code);
    p.setName(code);
    p.setStatus(ProductStatus.ACTIVE);
    p.setCategoryId(categoryId);
    p.setOwnerUserId(ownerUserId);
    return productRepo.saveAndFlush(p).getId();
  }

  private Long createModule(String code, String name, Long productId, Long ownerUserId) {
    ProductModule m = new ProductModule();
    m.setCode(code);
    m.setName(name);
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(productId);
    m.setOwnerUserId(ownerUserId);
    return moduleRepo.saveAndFlush(m).getId();
  }

  /** TC-FEAT-001: minimal payload → 201, default status=PLANNING, moduleName enrichment. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndEnrichment() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-FIN", uid);
    Long pid = createProduct("PROD-PAY", cid, uid);
    Long mid = createModule("MOD-WALLET", "钱包", pid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-RECHARGE");
    body.put("name", "充值");
    body.put("moduleId", mid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/features/\\d+")))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.moduleCode").value("MOD-WALLET"))
        .andExpect(jsonPath("$.moduleName").value("钱包"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"));
  }

  /** TC-FEAT-002: moduleId not found → 400. */
  @Test
  void post_unknownModule_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-X");
    body.put("name", "x");
    body.put("moduleId", 999_999L);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("module not found")));
  }

  /** TC-FEAT-003: duplicate code → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-A", uid);
    Long pid = createProduct("PROD-A", cid, uid);
    Long mid = createModule("MOD-A", "A", pid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-DUP");
    body.put("name", "x");
    body.put("moduleId", mid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-FEAT-004: invalid status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-Z", uid);
    Long pid = createProduct("PROD-Z", cid, uid);
    Long mid = createModule("MOD-Z", "Z", pid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-Z");
    body.put("name", "x");
    body.put("moduleId", mid);
    body.put("ownerUserId", uid);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-FEAT-005: missing required fields → 400 fieldErrors (name + moduleId + ownerUserId). */
  @Test
  void post_missingRequired_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-MISS");
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("moduleId")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-FEAT-006: createBy auto-injected. */
  @Test
  void post_createBy_autoInjected() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-CB", uid);
    Long pid = createProduct("PROD-CB", cid, uid);
    Long mid = createModule("MOD-CB", "CB", pid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-CB");
    body.put("name", "x");
    body.put("moduleId", mid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }
}
