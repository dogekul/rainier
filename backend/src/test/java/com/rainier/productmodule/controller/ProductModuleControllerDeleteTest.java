/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.domain.ProductCategoryStatus;
import com.rainier.productcategory.repository.ProductCategoryRepository;
import com.rainier.feature.domain.Feature;
import com.rainier.feature.domain.FeatureStatus;
import com.rainier.feature.repository.FeatureRepository;
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

/** TC-PMOD-011 (FK ref test TC-PMOD-012 lives in M09 FeatureControllerDeleteFkTest). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductModuleRepository repo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    featureRepo.deleteAll();
    repo.deleteAll();
    productRepo.deleteAll();
    categoryRepo.deleteAll();
    userRepo.deleteAll();
  }

  /** TC-PMOD-011: soft-delete returns 204; subsequent GET returns 404. */
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

    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-D");
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
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    mockMvc.perform(delete("/api/product-modules/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/product-modules/" + id)).andExpect(status().isNotFound());
  }

  /** TC-PMOD-012 (M09 retro-fit): 有 Feature 引用 → 409 "module has linked features". */
  @Test
  void delete_withLinkedFeature_returns409() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long uid = userRepo.saveAndFlush(u).getId();

    ProductCategory c = new ProductCategory();
    c.setCode("CAT-FK");
    c.setName("x");
    c.setStatus(ProductCategoryStatus.ACTIVE);
    c.setOwnerUserId(uid);
    Long cid = categoryRepo.saveAndFlush(c).getId();

    Product p = new Product();
    p.setCode("PROD-FK");
    p.setName("x");
    p.setStatus(ProductStatus.ACTIVE);
    p.setCategoryId(cid);
    p.setOwnerUserId(uid);
    Long pid = productRepo.saveAndFlush(p).getId();

    ProductModule m = new ProductModule();
    m.setCode("MOD-FK");
    m.setName("x");
    m.setStatus(ProductModuleStatus.PLANNING);
    m.setProductId(pid);
    m.setOwnerUserId(uid);
    Long mid = repo.saveAndFlush(m).getId();

    Feature f = new Feature();
    f.setCode("FEAT-FK");
    f.setName("x");
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(mid);
    f.setOwnerUserId(uid);
    featureRepo.saveAndFlush(f);

    mockMvc
        .perform(delete("/api/product-modules/" + mid))
        .andExpect(status().isConflict())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath(
                    "$.message", Matchers.startsWith("module has linked features")));
  }
}
