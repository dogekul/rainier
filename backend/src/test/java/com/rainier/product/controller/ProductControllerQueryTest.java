/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

/** TC-PROD-007 (15-field detail loop) + TC-PROD-008 (filter by categoryId). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerQueryTest {

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

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
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

  private Long createProduct(Long uid, Long cid, String code) throws Exception {
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

  /** TC-PROD-007: GET detail returns all 15 fields including categoryCode/categoryName. */
  @Test
  void get_existingId_returnsFullDetailWithAllEnrichment() throws Exception {
    Long uid = createUser();
    Long cid = createCategory("CAT-FIN", "金融产品", uid);
    Long id = createProduct(uid, cid, "PROD-Q1");
    MvcResult res =
        mockMvc.perform(get("/api/products/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id", "code", "name", "description", "status",
      "categoryId", "categoryCode", "categoryName",
      "ownerUserId", "ownerName", "ownerLoginName",
      "createTime", "updateTime", "createBy", "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-PROD-008: list filtered by categoryId returns only matching. */
  @Test
  void getList_filterByCategoryId_returnsOnlyMatching() throws Exception {
    Long uid = createUser();
    Long catA = createCategory("CAT-A", "A", uid);
    Long catB = createCategory("CAT-B", "B", uid);
    createProduct(uid, catA, "PROD-A1");
    createProduct(uid, catA, "PROD-A2");
    createProduct(uid, catB, "PROD-B1");
    MvcResult res =
        mockMvc
            .perform(get("/api/products?categoryId=" + catA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andReturn();
    JsonNode list = json.readTree(res.getResponse().getContentAsString()).get("content");
    for (JsonNode item : list) {
      org.junit.jupiter.api.Assertions.assertEquals(
          catA.longValue(), item.get("categoryId").asLong());
    }
  }
}
