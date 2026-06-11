/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
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

/** TC-FEAT-007 (15-field detail loop) + TC-FEAT-008 (filter by moduleId). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private FeatureRepository repo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
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

  private Long createProduct(String code, Long ownerUserId) {
    Product p = new Product();
    p.setCode(code);
    p.setName(code);
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(ownerUserId);
    return productRepo.saveAndFlush(p).getId();
  }

  private Long createModule(String code, Long productId, Long ownerUserId) {
    ProductModule m = new ProductModule();
    m.setCode(code);
    m.setName(code);
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(productId);
    m.setOwnerUserId(ownerUserId);
    return moduleRepo.saveAndFlush(m).getId();
  }

  private Long createFeature(Long uid, Long mid, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
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
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-FEAT-007: GET detail returns all 15 fields including moduleCode/moduleName. */
  @Test
  void get_existingId_returnsFullDetailWithAllEnrichment() throws Exception {
    Long uid = createUser();
    Long pid = createProduct("PROD-Q", uid);
    Long mid = createModule("MOD-Q", pid, uid);
    Long id = createFeature(uid, mid, "FEAT-Q1");
    MvcResult res =
        mockMvc.perform(get("/api/features/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id", "code", "name", "description", "status",
      "moduleId", "moduleCode", "moduleName",
      "ownerUserId", "ownerName", "ownerLoginName",
      "createTime", "updateTime", "createBy", "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-FEAT-008: list filtered by moduleId returns only matching. */
  @Test
  void getList_filterByModuleId_returnsOnlyMatching() throws Exception {
    Long uid = createUser();
    Long pid = createProduct("PROD-F", uid);
    Long modA = createModule("MOD-A", pid, uid);
    Long modB = createModule("MOD-B", pid, uid);
    createFeature(uid, modA, "FEAT-A1");
    createFeature(uid, modA, "FEAT-A2");
    createFeature(uid, modB, "FEAT-B1");
    MvcResult res =
        mockMvc
            .perform(get("/api/features?moduleId=" + modA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2))
            .andReturn();
    JsonNode list = json.readTree(res.getResponse().getContentAsString()).get("content");
    for (JsonNode item : list) {
      org.junit.jupiter.api.Assertions.assertEquals(
          modA.longValue(), item.get("moduleId").asLong());
    }
  }
}
