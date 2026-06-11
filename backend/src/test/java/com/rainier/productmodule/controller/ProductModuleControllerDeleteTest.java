/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.feature.domain.Feature;
import com.rainier.feature.domain.FeatureStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** TC-PMOD-022..025 — bidirectional delete protection (features first, then sub-modules). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductModuleRepository repo;
  @Autowired private ProductRepository productRepo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private UserRepository userRepo;

  private Long uid;
  private Long pid;

  @BeforeEach
  void seed() {
    featureRepo.deleteAll();
    repo.deleteAll();
    productRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
    Product p = new Product();
    p.setCode("PROD-DEL");
    p.setName("x");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    pid = productRepo.saveAndFlush(p).getId();
  }

  private Long seedModule(String code, Long parentId) {
    ProductModule m = new ProductModule();
    m.setCode(code);
    m.setName(code);
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(pid);
    m.setParentId(parentId);
    m.setOwnerUserId(uid);
    return repo.saveAndFlush(m).getId();
  }

  private void seedFeature(String code, Long moduleId) {
    Feature f = new Feature();
    f.setCode(code);
    f.setName(code);
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(moduleId);
    f.setOwnerUserId(uid);
    featureRepo.saveAndFlush(f);
  }

  /** TC-PMOD-022: 无 Feature 无子模块 → 204 + GET 404. */
  @Test
  void delete_noReferences_returns204AndGet404() throws Exception {
    Long id = seedModule("MOD-FREE", null);
    mockMvc.perform(delete("/api/product-modules/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/product-modules/" + id)).andExpect(status().isNotFound());
  }

  /** TC-PMOD-023: 有 Feature 引用 → 409 "module has linked features". */
  @Test
  void delete_withLinkedFeature_returns409() throws Exception {
    Long id = seedModule("MOD-FEAT", null);
    seedFeature("FEAT-X", id);
    mockMvc
        .perform(delete("/api/product-modules/" + id))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("module has linked features")));
  }

  /** TC-PMOD-024: 有子 Module → 409 "module has linked sub-modules". */
  @Test
  void delete_withChildModule_returns409() throws Exception {
    Long parent = seedModule("MOD-PARENT", null);
    seedModule("MOD-CHILD", parent);
    mockMvc
        .perform(delete("/api/product-modules/" + parent))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("module has linked sub-modules")));
  }

  /** TC-PMOD-025: 同时有 Feature 和子 Module → 先报 features（检查顺序固定）. */
  @Test
  void delete_withFeatureAndChildModule_reportsFeaturesFirst() throws Exception {
    Long parent = seedModule("MOD-BOTH", null);
    seedModule("MOD-BOTH-CHILD", parent);
    seedFeature("FEAT-BOTH", parent);
    mockMvc
        .perform(delete("/api/product-modules/" + parent))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("module has linked features")));
  }
}
