/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.domain.ProductModuleStatus;
import com.rainier.productmodule.repository.ProductModuleRepository;
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

/** TC-PROD-009 (soft-delete 204) + TC-PROD-010 (linked module 409). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductRepository repo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    moduleRepo.deleteAll();
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

  /** TC-PROD-009: soft-delete returns 204; subsequent GET returns 404. */
  @Test
  void delete_softDeletes_returns204AndGet404() throws Exception {
    Long uid = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROD-D");
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
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
  }

  /** TC-PROD-010: 有 ProductModule 引用 → 409 "product has linked modules". */
  @Test
  void delete_withLinkedModule_returns409() throws Exception {
    Long uid = createUser();

    Product p = new Product();
    p.setCode("PROD-FK");
    p.setName("x");
    p.setStatus(com.rainier.product.domain.ProductStatus.PLANNING);
    p.setOwnerUserId(uid);
    Long pid = repo.saveAndFlush(p).getId();

    ProductModule m = new ProductModule();
    m.setCode("MOD-FK");
    m.setName("x");
    m.setStatus(ProductModuleStatus.PLANNING);
    m.setProductId(pid);
    m.setOwnerUserId(uid);
    moduleRepo.saveAndFlush(m);

    mockMvc
        .perform(delete("/api/products/" + pid))
        .andExpect(status().isConflict())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                "$.message", Matchers.startsWith("product has linked modules")));
  }
}
