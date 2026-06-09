/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.rainier.productmodule.domain.ProductModule;
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
import org.springframework.test.web.servlet.MvcResult;

/** TC-PMOD-009 + TC-PMOD-010 (productId immutable). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerUpdateTest {

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

  private Long createProduct(String code, Long categoryId, Long ownerUserId) {
    Product p = new Product();
    p.setCode(code);
    p.setName(code);
    p.setStatus(ProductStatus.ACTIVE);
    p.setCategoryId(categoryId);
    p.setOwnerUserId(ownerUserId);
    return productRepo.saveAndFlush(p).getId();
  }

  private Long create(Long uid, Long pid, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "x");
    body.put("productId", pid);
    body.put("ownerUserId", uid);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/product-modules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-PMOD-009: PUT updates status + owner transfer + enrichment follows. */
  @Test
  void put_updateStatusAndOwner_returns200() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long lili = createUser("lili", "黎立");
    Long cid = createCategory("CAT-U", alice);
    Long pid = createProduct("PROD-U", cid, alice);
    Long id = create(alice, pid, "MOD-U1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-U1");
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", lili);
    mockMvc
        .perform(
            put("/api/product-modules/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }

  /** TC-PMOD-010: PUT body containing productId is silently dropped; DB productId unchanged. */
  @Test
  void put_payloadWithProductId_silentlyDropped_productIdUnchanged() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long cid = createCategory("CAT-IMM", alice);
    Long prodA = createProduct("PROD-IMM-A", cid, alice);
    Long prodB = createProduct("PROD-IMM-B", cid, alice);
    Long id = create(alice, prodA, "MOD-IMM");
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-IMM");
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", alice);
    body.put("productId", prodB); // attempt to switch — must be ignored
    mockMvc
        .perform(
            put("/api/product-modules/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
    ProductModule fromDb = repo.findById(id).orElseThrow(IllegalStateException::new);
    org.junit.jupiter.api.Assertions.assertEquals(prodA, fromDb.getProductId());
  }
}
