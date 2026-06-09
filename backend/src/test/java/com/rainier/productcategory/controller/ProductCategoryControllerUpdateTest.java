/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.MvcResult;

/** TC-PCAT-009/010. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCategoryControllerUpdateTest {

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

  private Long create(Long uid, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "x");
    body.put("ownerUserId", uid);
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

  /** TC-PCAT-009: 更新 status + owner 转移 + 富化跟随. */
  @Test
  void put_updateStatusAndOwner_returns200() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long lili = createUser("lili", "黎立");
    Long id = create(alice, "CAT-U1");
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-U1");
    body.put("name", "X");
    body.put("status", "ARCHIVED");
    body.put("ownerUserId", lili);
    mockMvc
        .perform(
            put("/api/product-categories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ARCHIVED"))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }

  /** TC-PCAT-010: 新 ownerUserId 不存在 → 400. */
  @Test
  void put_unknownNewOwner_returns400() throws Exception {
    Long alice = createUser("alice", "Alice");
    Long id = create(alice, "CAT-U2");
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-U2");
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            put("/api/product-categories/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }
}
