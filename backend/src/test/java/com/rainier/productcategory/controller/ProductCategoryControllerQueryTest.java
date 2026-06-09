/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;

/** TC-PCAT-007 (12-field loop) + TC-PCAT-008 (status filter). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCategoryControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductCategoryRepository repo;
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

  private Long createCategory(Long uid, String code, String status) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "x");
    body.put("ownerUserId", uid);
    if (status != null) body.put("status", status);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/product-categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-PCAT-007: GET 详情 12 字段全有. */
  @Test
  void get_existingId_returnsFullDetailWithOwnerEnrichment() throws Exception {
    Long uid = createUser();
    Long id = createCategory(uid, "CAT-Q1", null);
    MvcResult res = mockMvc.perform(get("/api/product-categories/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id", "code", "name", "description", "status", "ownerUserId", "ownerName", "ownerLoginName",
      "createTime", "updateTime", "createBy", "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-PCAT-008: 按 status 过滤列表. */
  @Test
  void getList_filterByStatus_returnsOnlyMatching() throws Exception {
    Long uid = createUser();
    createCategory(uid, "CAT-A1", "ACTIVE");
    createCategory(uid, "CAT-A2", "ACTIVE");
    createCategory(uid, "CAT-AR", "ARCHIVED");
    mockMvc
        .perform(get("/api/product-categories?status=ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
  }
}
