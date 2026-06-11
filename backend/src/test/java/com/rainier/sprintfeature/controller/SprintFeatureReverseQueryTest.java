/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.controller;

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
import com.rainier.common.domain.Priority;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.sprintfeature.domain.SprintFeatureLink;
import com.rainier.sprintfeature.repository.SprintFeatureLinkRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** TC-SF-REV-001..008 — the three reverse-query endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintFeatureReverseQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private SprintFeatureLinkRepository repo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;

  private Long uid;
  private Long productId;
  private Long moduleId;

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
    Product p = new Product();
    p.setCode("PROD-REV");
    p.setName("产品");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    productId = productRepo.saveAndFlush(p).getId();
    ProductModule m = new ProductModule();
    m.setCode("MOD-REV");
    m.setName("模块");
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(productId);
    m.setOwnerUserId(uid);
    moduleId = moduleRepo.saveAndFlush(m).getId();
  }

  private Long createFeature(String code) {
    Feature f = new Feature();
    f.setCode(code);
    f.setName("功能" + code);
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(moduleId);
    f.setOwnerUserId(uid);
    return featureRepo.saveAndFlush(f).getId();
  }

  private Long createRequirement(String code) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("需求");
    r.setOwnerUserId(uid);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long createSprint(String code, Long requirementId) {
    Sprint s = new Sprint();
    s.setCode(code);
    s.setName("迭代");
    s.setStatus(SprintStatus.PLANNING);
    s.setRequirementId(requirementId);
    s.setProductId(productId);
    s.setOwnerUserId(uid);
    return sprintRepo.saveAndFlush(s).getId();
  }

  private void link(Long sprintId, Long featureId) {
    SprintFeatureLink l = new SprintFeatureLink();
    l.setSprintId(sprintId);
    l.setFeatureId(featureId);
    repo.saveAndFlush(l);
  }

  /** TC-SF-REV-001: sprint→features 列表. */
  @Test
  void getSprintFeatures_listsFeatures() throws Exception {
    Long reqId = createRequirement("REQ-1");
    Long sId = createSprint("SPR-1", reqId);
    Long f1 = createFeature("FEAT-1");
    Long f2 = createFeature("FEAT-2");
    link(sId, f1);
    link(sId, f2);
    mockMvc
        .perform(get("/api/sprints/" + sId + "/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath(
                "$[*].featureId",
                org.hamcrest.Matchers.containsInAnyOrder(f1.intValue(), f2.intValue())))
        .andExpect(
            jsonPath(
                "$[*].code", org.hamcrest.Matchers.containsInAnyOrder("FEAT-1", "FEAT-2")))
        .andExpect(jsonPath("$[0].name").exists())
        .andExpect(jsonPath("$[0].moduleId").exists());
  }

  /** TC-SF-REV-002: sprint→features sprint 不存在 404. */
  @Test
  void getSprintFeatures_unknownSprint_returns404() throws Exception {
    mockMvc
        .perform(get("/api/sprints/999999/features"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message", org.hamcrest.Matchers.startsWith("sprint not found")));
  }

  /** TC-SF-REV-003: feature→sprints 列表. */
  @Test
  void getFeatureSprints_listsSprints() throws Exception {
    Long reqId = createRequirement("REQ-3");
    Long s1 = createSprint("SPR-3A", reqId);
    Long s2 = createSprint("SPR-3B", reqId);
    Long f = createFeature("FEAT-3");
    link(s1, f);
    link(s2, f);
    mockMvc
        .perform(get("/api/features/" + f + "/sprints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(
            jsonPath(
                "$[*].sprintId",
                org.hamcrest.Matchers.containsInAnyOrder(s1.intValue(), s2.intValue())))
        .andExpect(
            jsonPath(
                "$[*].code", org.hamcrest.Matchers.containsInAnyOrder("SPR-3A", "SPR-3B")))
        .andExpect(jsonPath("$[0].status").exists());
  }

  /** TC-SF-REV-004: feature→sprints 空数组. */
  @Test
  void getFeatureSprints_noLinks_returnsEmpty() throws Exception {
    Long f = createFeature("FEAT-4");
    mockMvc
        .perform(get("/api/features/" + f + "/sprints"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** TC-SF-REV-005: feature→sprints feature 不存在 404. */
  @Test
  void getFeatureSprints_unknownFeature_returns404() throws Exception {
    mockMvc
        .perform(get("/api/features/999999/sprints"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message", org.hamcrest.Matchers.startsWith("feature not found")));
  }

  /** TC-SF-REV-006: requirement→features 2 跳汇总去重. */
  @Test
  void getRequirementFeatures_dedupesAcrossSprints() throws Exception {
    Long reqId = createRequirement("REQ-6");
    Long s1 = createSprint("SPR-6A", reqId);
    Long s2 = createSprint("SPR-6B", reqId);
    Long f1 = createFeature("FEAT-6-1");
    Long f2 = createFeature("FEAT-6-2");
    Long f3 = createFeature("FEAT-6-3");
    link(s1, f1);
    link(s1, f2);
    link(s2, f2); // duplicate F2 across sprints
    link(s2, f3);
    mockMvc
        .perform(get("/api/requirements/" + reqId + "/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[*].featureId", org.hamcrest.Matchers.containsInAnyOrder(
            f1.intValue(), f2.intValue(), f3.intValue())));
  }

  /** TC-SF-REV-007: requirement→features 空数组. */
  @Test
  void getRequirementFeatures_noLinks_returnsEmpty() throws Exception {
    Long reqId = createRequirement("REQ-7");
    createSprint("SPR-7", reqId); // sprint exists but no links
    mockMvc
        .perform(get("/api/requirements/" + reqId + "/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** TC-SF-REV-008: requirement→features requirement 不存在 404. */
  @Test
  void getRequirementFeatures_unknownRequirement_returns404() throws Exception {
    mockMvc
        .perform(get("/api/requirements/999999/features"))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.message", org.hamcrest.Matchers.startsWith("requirement not found")));
  }
}
