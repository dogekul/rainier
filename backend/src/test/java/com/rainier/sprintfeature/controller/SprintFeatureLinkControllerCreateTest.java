/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.feature.domain.Feature;
import com.rainier.feature.domain.FeatureStatus;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.domain.ProductModuleStatus;
import com.rainier.productmodule.repository.ProductModuleRepository;
import com.rainier.common.domain.Priority;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.sprintfeature.repository.SprintFeatureLinkRepository;
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

/** TC-SF-001..006 — create link: lazy productId establish + product consistency + uniqueness. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintFeatureLinkControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private SprintFeatureLinkRepository repo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long uid;

  @BeforeEach
  void seed() {
    repo.deleteAll();
    featureRepo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
  }

  private Long createSprint(String code) {
    Requirement r = new Requirement();
    r.setCode("REQ-" + code);
    r.setTitle("需求");
    r.setOwnerUserId(uid);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    Long reqId = requirementRepo.saveAndFlush(r).getId();
    Sprint s = new Sprint();
    s.setCode(code);
    s.setName("迭代");
    s.setStatus(SprintStatus.PLANNING);
    s.setRequirementId(reqId);
    s.setOwnerUserId(uid);
    return sprintRepo.saveAndFlush(s).getId();
  }

  /** Returns featureId; product/module created under productCode. */
  private Long createFeature(String productCode, String featureCode) {
    Product p = new Product();
    p.setCode(productCode);
    p.setName(productCode);
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    Long pid = productRepo.saveAndFlush(p).getId();
    ProductModule m = new ProductModule();
    m.setCode("MOD-" + featureCode);
    m.setName("模块");
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(pid);
    m.setOwnerUserId(uid);
    Long mid = moduleRepo.saveAndFlush(m).getId();
    Feature f = new Feature();
    f.setCode(featureCode);
    f.setName("功能");
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(mid);
    f.setOwnerUserId(uid);
    return featureRepo.saveAndFlush(f).getId();
  }

  private Long featureUnderProduct(Long productId, String featureCode) {
    ProductModule m = new ProductModule();
    m.setCode("MOD-" + featureCode);
    m.setName("模块");
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(productId);
    m.setOwnerUserId(uid);
    Long mid = moduleRepo.saveAndFlush(m).getId();
    Feature f = new Feature();
    f.setCode(featureCode);
    f.setName("功能");
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(mid);
    f.setOwnerUserId(uid);
    return featureRepo.saveAndFlush(f).getId();
  }

  private Long productOfFeature(Long featureId) {
    Long moduleId = featureRepo.findById(featureId).orElseThrow(IllegalStateException::new).getModuleId();
    return moduleRepo.findById(moduleId).orElseThrow(IllegalStateException::new).getProductId();
  }

  private void postLink(Long sprintId, Long featureId, int expectedStatus) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("sprintId", sprintId);
    body.put("featureId", featureId);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().is(expectedStatus));
  }

  /** TC-SF-001: 首个 feature 挂载触发 productId 惰性建立. */
  @Test
  void post_firstFeature_establishesSprintProductId() throws Exception {
    Long sprintId = createSprint("SPR-A");
    Long featureId = createFeature("PROD-A", "FEAT-A1");
    Long expectedProduct = productOfFeature(featureId);
    ObjectNode body = json.createObjectNode();
    body.put("sprintId", sprintId);
    body.put("featureId", featureId);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/sprint-features/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.sprintId").value(sprintId))
        .andExpect(jsonPath("$.featureId").value(featureId))
        .andExpect(jsonPath("$.createTime").exists());
    mockMvc
        .perform(get("/api/sprints/" + sprintId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.productId").value(expectedProduct));
  }

  /** TC-SF-002: 第二个同产品 feature 挂载成功. */
  @Test
  void post_secondFeatureSameProduct_returns201() throws Exception {
    Long sprintId = createSprint("SPR-B");
    Long f1 = createFeature("PROD-B", "FEAT-B1");
    Long product = productOfFeature(f1);
    Long f2 = featureUnderProduct(product, "FEAT-B2");
    postLink(sprintId, f1, 201);
    postLink(sprintId, f2, 201);
  }

  /** TC-SF-003: 跨产品 feature 挂载被拒. */
  @Test
  void post_crossProductFeature_returns400() throws Exception {
    Long sprintId = createSprint("SPR-C");
    Long f1 = createFeature("PROD-C1", "FEAT-C1");
    Long g = createFeature("PROD-C2", "FEAT-C2"); // different product
    postLink(sprintId, f1, 201);
    ObjectNode body = json.createObjectNode();
    body.put("sprintId", sprintId);
    body.put("featureId", g);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("feature must belong to the sprint's product")));
  }

  /** TC-SF-004: 唯一性冲突 409. */
  @Test
  void post_duplicateLink_returns409() throws Exception {
    Long sprintId = createSprint("SPR-D");
    Long f = createFeature("PROD-D", "FEAT-D1");
    postLink(sprintId, f, 201);
    ObjectNode body = json.createObjectNode();
    body.put("sprintId", sprintId);
    body.put("featureId", f);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("link already exists")));
  }

  /** TC-SF-005: sprintId 不存在被拒. */
  @Test
  void post_unknownSprint_returns400() throws Exception {
    Long f = createFeature("PROD-E", "FEAT-E1");
    ObjectNode body = json.createObjectNode();
    body.put("sprintId", 999999L);
    body.put("featureId", f);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("sprint not found")));
  }

  /** TC-SF-006: featureId 不存在被拒. */
  @Test
  void post_unknownFeature_returns400() throws Exception {
    Long sprintId = createSprint("SPR-F");
    ObjectNode body = json.createObjectNode();
    body.put("sprintId", sprintId);
    body.put("featureId", 999999L);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("feature not found")));
  }
}
