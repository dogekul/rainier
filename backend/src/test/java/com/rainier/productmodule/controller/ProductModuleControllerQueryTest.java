/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

/** TC-PMOD-007 (15-field detail loop) + TC-PMOD-008 (filter by productId). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerQueryTest {

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

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
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

  private Long createModule(Long uid, Long pid, String code) throws Exception {
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

  /** TC-PMOD-007: GET detail returns all 15 fields including productCode/productName. */
  @Test
  void get_existingId_returnsFullDetailWithAllEnrichment() throws Exception {
    Long uid = createUser();
    Long cid = createCategory("CAT-Q", uid);
    Long pid = createProduct("PROD-Q", cid, uid);
    Long id = createModule(uid, pid, "MOD-Q1");
    MvcResult res =
        mockMvc.perform(get("/api/product-modules/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id", "code", "name", "description", "status",
      "productId", "productCode", "productName",
      "ownerUserId", "ownerName", "ownerLoginName",
      "createTime", "updateTime", "createBy", "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-PMOD-008: list filtered by productId returns only matching. */
  @Test
  void getList_filterByProductId_returnsOnlyMatching() throws Exception {
    Long uid = createUser();
    Long cid = createCategory("CAT-F", uid);
    Long prodA = createProduct("PROD-A", cid, uid);
    Long prodB = createProduct("PROD-B", cid, uid);
    createModule(uid, prodA, "MOD-A1");
    createModule(uid, prodA, "MOD-A2");
    createModule(uid, prodB, "MOD-B1");
    MvcResult res =
        mockMvc
            .perform(get("/api/product-modules?productId=" + prodA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andReturn();
    JsonNode list = json.readTree(res.getResponse().getContentAsString()).get("content");
    for (JsonNode item : list) {
      org.junit.jupiter.api.Assertions.assertEquals(
          prodA.longValue(), item.get("productId").asLong());
    }
  }
}
