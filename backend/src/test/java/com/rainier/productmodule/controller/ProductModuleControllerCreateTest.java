/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.domain.ProductCategoryStatus;
import com.rainier.productcategory.repository.ProductCategoryRepository;
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

/** TC-PMOD-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductModuleRepository repo;
  @Autowired private ProductRepository productRepo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
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

  private Long createProduct(String code, String name, Long categoryId, Long ownerUserId) {
    Product p = new Product();
    p.setCode(code);
    p.setName(name);
    p.setStatus(ProductStatus.ACTIVE);
    p.setCategoryId(categoryId);
    p.setOwnerUserId(ownerUserId);
    return productRepo.saveAndFlush(p).getId();
  }

  /** TC-PMOD-001: minimal payload → 201, default status=PLANNING, productName enrichment. */
  @Test
  void post_minimalPayload_returns201WithDefaultsAndEnrichment() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-FIN", uid);
    Long pid = createProduct("PROD-PAY", "支付平台", cid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-WALLET");
    body.put("name", "钱包");
    body.put("productId", pid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/product-modules/\\d+")))
        .andExpect(jsonPath("$.status").value("PLANNING"))
        .andExpect(jsonPath("$.productCode").value("PROD-PAY"))
        .andExpect(jsonPath("$.productName").value("支付平台"))
        .andExpect(jsonPath("$.ownerName").value("Alice"))
        .andExpect(jsonPath("$.ownerLoginName").value("alice"));
  }

  /** TC-PMOD-002: productId not found → 400. */
  @Test
  void post_unknownProduct_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-X");
    body.put("name", "x");
    body.put("productId", 999_999L);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("product not found")));
  }

  /** TC-PMOD-003: duplicate code → 409. */
  @Test
  void post_duplicateCode_returns409() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-A", uid);
    Long pid = createProduct("PROD-DUP", "P", cid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-DUP");
    body.put("name", "x");
    body.put("productId", pid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  /** TC-PMOD-004: invalid status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-Z", uid);
    Long pid = createProduct("PROD-Z", "Z", cid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-Z");
    body.put("name", "x");
    body.put("productId", pid);
    body.put("ownerUserId", uid);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-PMOD-005: missing required fields → 400 fieldErrors (name + productId + ownerUserId). */
  @Test
  void post_missingRequired_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-MISS");
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("productId")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-PMOD-006: createBy auto-injected. */
  @Test
  void post_createBy_autoInjected() throws Exception {
    Long uid = createUser("alice", "Alice");
    Long cid = createCategory("CAT-CB", uid);
    Long pid = createProduct("PROD-CB", "CB", cid, uid);
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-CB");
    body.put("name", "x");
    body.put("productId", pid);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.createBy").exists());
  }
}
