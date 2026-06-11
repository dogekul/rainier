/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.perf;

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

/** TC-PERF-FEAT-001: 2 page + 1 user + 1 module = 4. */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeatureListSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private FeatureRepository repo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    repo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
    userRepo.deleteAll();
    Long[] uids = new Long[4];
    for (int i = 0; i < 4; i++) {
      User u = new User();
      u.setLoginName("u" + i);
      u.setName("U " + i);
      u.setIsInternal(true);
      u.setEnabled(true);
      uids[i] = userRepo.saveAndFlush(u).getId();
    }
    Product p = new Product();
    p.setCode("PROD-F-PERF");
    p.setName("x");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uids[0]);
    Long pid = productRepo.saveAndFlush(p).getId();
    Long[] mids = new Long[4];
    for (int i = 0; i < 4; i++) {
      ProductModule m = new ProductModule();
      m.setCode("MOD-F-PERF-" + i);
      m.setName("x");
      m.setStatus(ProductModuleStatus.ACTIVE);
      m.setProductId(pid);
      m.setOwnerUserId(uids[i % 4]);
      mids[i] = moduleRepo.saveAndFlush(m).getId();
    }
    for (int i = 0; i < 20; i++) {
      Feature f = new Feature();
      f.setCode("FEAT-PERF-" + i);
      f.setName("F " + i);
      f.setStatus(FeatureStatus.PLANNING);
      f.setModuleId(mids[i % 4]);
      f.setOwnerUserId(uids[i % 4]);
      repo.saveAndFlush(f);
    }
  }

  @Test
  void list_size20_executesBoundedSqlCount() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "stats must be enabled");
    stats.clear();
    mockMvc
        .perform(get("/api/features?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].moduleName").exists());
    long n = stats.getPrepareStatementCount();
    assertTrue(n >= 4L && n <= 5L, "expected 4..5 statements, got " + n);
  }
}
