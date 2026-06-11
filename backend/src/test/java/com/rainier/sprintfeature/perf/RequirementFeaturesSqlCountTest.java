/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.perf;

import static org.junit.jupiter.api.Assertions.assertTrue;
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
import javax.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * TC-PERF-SF-REV-001: GET /api/requirements/{id}/features 2-hop rollup stays bounded.
 * existsById(1) + findByRequirementId(1) + findBySprintId × N sprints + feature batch(1).
 * With 5 sprints → ~8. Range assert ≥2 ∧ ≤8 guards stats-off false green + per-feature regressions.
 */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementFeaturesSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private SprintFeatureLinkRepository repo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;

  private Long requirementId;

  @BeforeEach
  @Transactional
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
    Long uid = userRepo.saveAndFlush(u).getId();
    Product p = new Product();
    p.setCode("PROD-RF");
    p.setName("产品");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    Long pid = productRepo.saveAndFlush(p).getId();
    ProductModule m = new ProductModule();
    m.setCode("MOD-RF");
    m.setName("模块");
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(pid);
    m.setOwnerUserId(uid);
    Long mid = moduleRepo.saveAndFlush(m).getId();
    Requirement r = new Requirement();
    r.setCode("REQ-RF");
    r.setTitle("需求");
    r.setOwnerUserId(uid);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    requirementId = requirementRepo.saveAndFlush(r).getId();
    // 5 sprints, each with 2 features → 10 features total.
    for (int i = 0; i < 5; i++) {
      Sprint s = new Sprint();
      s.setCode("SPR-RF-" + i);
      s.setName("迭代 " + i);
      s.setStatus(SprintStatus.PLANNING);
      s.setRequirementId(requirementId);
      s.setProductId(pid);
      s.setOwnerUserId(uid);
      Long sid = sprintRepo.saveAndFlush(s).getId();
      for (int j = 0; j < 2; j++) {
        Feature f = new Feature();
        f.setCode("FEAT-RF-" + i + "-" + j);
        f.setName("功能");
        f.setStatus(FeatureStatus.PLANNING);
        f.setModuleId(mid);
        f.setOwnerUserId(uid);
        Long fid = featureRepo.saveAndFlush(f).getId();
        SprintFeatureLink l = new SprintFeatureLink();
        l.setSprintId(sid);
        l.setFeatureId(fid);
        repo.saveAndFlush(l);
      }
    }
  }

  @Test
  void requirementFeatures_executesBoundedSqlCount() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "stats must be enabled");
    stats.clear();
    mockMvc
        .perform(get("/api/requirements/" + requirementId + "/features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(10));
    long n = stats.getPrepareStatementCount();
    assertTrue(n >= 2L && n <= 8L, "expected 2..8 statements, got " + n);
  }
}
