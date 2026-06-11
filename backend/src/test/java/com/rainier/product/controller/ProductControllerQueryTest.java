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

/** TC-PROD-006 (detail field set, no category fields) + TC-PROD-007 (categoryId param ignored). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository repo;
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

  private Long createProduct(Long uid, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "x");
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

  /** TC-PROD-006: GET detail 12 fields — category 富化字段已移除 (v0.0.13). */
  @Test
  void get_existingId_returnsDetailWithoutCategoryFields() throws Exception {
    Long uid = createUser();
    Long id = createProduct(uid, "PROD-Q1");
    MvcResult res =
        mockMvc.perform(get("/api/products/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id", "code", "name", "description", "status",
      "ownerUserId", "ownerName", "ownerLoginName",
      "createTime", "updateTime", "createBy", "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
    String[] removed = {"categoryId", "categoryCode", "categoryName"};
    for (String f : removed) {
      org.junit.jupiter.api.Assertions.assertFalse(body.has(f), "removed field present: " + f);
    }
  }

  /** TC-PROD-007: 废弃 categoryId 查询参数静默忽略 — 不过滤不报错. */
  @Test
  void getList_obsoleteCategoryIdParam_silentlyIgnored() throws Exception {
    Long uid = createUser();
    createProduct(uid, "PROD-A1");
    createProduct(uid, "PROD-A2");
    createProduct(uid, "PROD-B1");
    mockMvc
        .perform(get("/api/products?categoryId=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3));
  }

  /** status filter (v0.0.12 沿用). */
  @Test
  void getList_filterByStatus_returnsOnlyMatching() throws Exception {
    Long uid = createUser();
    createProduct(uid, "PROD-S1");
    mockMvc
        .perform(get("/api/products?status=PLANNING"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1));
    mockMvc
        .perform(get("/api/products?status=ARCHIVED"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(0));
  }
}
