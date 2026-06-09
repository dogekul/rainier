/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
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
import org.springframework.test.web.servlet.MvcResult;

/** TC-PROD-009 (status+owner) + TC-PROD-010 (categoryId immutable). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerUpdateTest {

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

  private Long create(Long uid, Long cid, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "x");
    body.put("categoryId", cid);
    body.put("ownerUserId", uid);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-PROD-009: PUT updates status + owner transfer + enrichment follows. */
  @Test
  void put_updateStatusAndOwner_returns200() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long lili = createUser("lili", "黎立");
    Long cid = createCategory("CAT-U", "U", alice);
    Long id = create(alice, cid, "PROD-U1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-U1");
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", lili);
    mockMvc
        .perform(
            put("/api/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }

  /**
   * TC-PROD-010: PUT body containing categoryId is silently dropped — Update DTO has no
   * categoryId setter so Jackson ignores it. DB categoryId stays unchanged.
   */
  @Test
  void put_payloadWithCategoryId_silentlyDropped_categoryIdUnchanged() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long catA = createCategory("CAT-IMM-A", "A", alice);
    Long catB = createCategory("CAT-IMM-B", "B", alice);
    Long id = create(alice, catA, "PROD-IMM");
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-IMM");
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", alice);
    body.put("categoryId", catB); // attempt to switch category — must be ignored
    mockMvc
        .perform(
            put("/api/products/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
    Product fromDb = repo.findById(id).orElseThrow(IllegalStateException::new);
    org.junit.jupiter.api.Assertions.assertEquals(catA, fromDb.getCategoryId());
  }
}
