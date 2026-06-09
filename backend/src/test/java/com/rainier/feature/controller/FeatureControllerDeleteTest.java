/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.domain.ProductCategoryStatus;
import com.rainier.productcategory.repository.ProductCategoryRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.domain.ProductModuleStatus;
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

/** TC-FEAT-011 (Feature is leaf — no downstream FK protection needed). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureRepository repo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
    categoryRepo.deleteAll();
    userRepo.deleteAll();
  }

  /** TC-FEAT-011: soft-delete returns 204; subsequent GET returns 404. */
  @Test
  void delete_softDeletes_returns204AndGet404() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long uid = userRepo.saveAndFlush(u).getId();

    ProductCategory c = new ProductCategory();
    c.setCode("CAT-D");
    c.setName("D");
    c.setStatus(ProductCategoryStatus.ACTIVE);
    c.setOwnerUserId(uid);
    Long cid = categoryRepo.saveAndFlush(c).getId();

    Product p = new Product();
    p.setCode("PROD-D");
    p.setName("D");
    p.setStatus(ProductStatus.ACTIVE);
    p.setCategoryId(cid);
    p.setOwnerUserId(uid);
    Long pid = productRepo.saveAndFlush(p).getId();

    ProductModule m = new ProductModule();
    m.setCode("MOD-D");
    m.setName("D");
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(pid);
    m.setOwnerUserId(uid);
    Long mid = moduleRepo.saveAndFlush(m).getId();

    ObjectNode body = json.createObjectNode();
    body.put("code", "FEAT-D");
    body.put("name", "x");
    body.put("moduleId", mid);
    body.put("ownerUserId", uid);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/api/features/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/features/" + id)).andExpect(status().isNotFound());
  }
}
