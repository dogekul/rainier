/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productcategory.repository.ProductCategoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TC-PCAT-011 (FK ref test in M09 ProductControllerDeleteFkTest). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCategoryControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductCategoryRepository repo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    productRepo.deleteAll();
    repo.deleteAll();
    userRepo.deleteAll();
  }

  /** TC-PCAT-011: 软删 + 后续 GET 404. */
  @Test
  void delete_softDeletes_returns204AndGet404() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long uid = userRepo.saveAndFlush(u).getId();
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-D");
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
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/api/product-categories/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/product-categories/" + id)).andExpect(status().isNotFound());
  }

  /** TC-PCAT-012 (M09 retro-fit): 有 Product 引用 → 409 "category has linked products". */
  @Test
  void delete_withLinkedProduct_returns409() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long uid = userRepo.saveAndFlush(u).getId();
    ObjectNode body = json.createObjectNode();
    body.put("code", "CAT-FK");
    body.put("name", "x");
    body.put("ownerUserId", uid);
    MvcResult res =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                        "/api/product-categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    Long catId = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
    // seed linked Product directly via repo
    Product p = new Product();
    p.setCode("PROD-FK");
    p.setName("x");
    p.setStatus(ProductStatus.PLANNING);
    p.setCategoryId(catId);
    p.setOwnerUserId(uid);
    productRepo.saveAndFlush(p);

    mockMvc
        .perform(delete("/api/product-categories/" + catId))
        .andExpect(status().isConflict())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.message", Matchers.startsWith("category has linked products")));
  }
}
