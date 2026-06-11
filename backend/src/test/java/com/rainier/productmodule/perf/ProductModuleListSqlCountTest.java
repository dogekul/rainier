/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.perf;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * TC-PERF-PMOD-001 (v0.0.13): page select + count + user batch + product batch + ≤2 ancestor
 * batch rounds = 4..6. Seeds a mixed flat/2-level tree so the ancestor batch actually runs
 * (陷阱 J: path enrich must be batched, never per-row walks).
 */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleListSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private ProductModuleRepository repo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    repo.deleteAll();
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
    p.setCode("PROD-PERF");
    p.setName("Perf");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uids[0]);
    Long pid = productRepo.saveAndFlush(p).getId();
    // 10 top-level roots; each root gets 1 child → 20 rows, 2-level tree.
    for (int i = 0; i < 10; i++) {
      ProductModule root = new ProductModule();
      root.setCode("MOD-ROOT-" + i);
      root.setName("Root " + i);
      root.setStatus(ProductModuleStatus.PLANNING);
      root.setProductId(pid);
      root.setOwnerUserId(uids[i % 4]);
      Long rootId = repo.saveAndFlush(root).getId();
      ProductModule child = new ProductModule();
      child.setCode("MOD-CHILD-" + i);
      child.setName("Child " + i);
      child.setStatus(ProductModuleStatus.PLANNING);
      child.setProductId(pid);
      child.setParentId(rootId);
      child.setOwnerUserId(uids[i % 4]);
      repo.saveAndFlush(child);
    }
  }

  @Test
  void list_size20_executesBoundedSqlCount() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "stats must be enabled");
    stats.clear();
    mockMvc
        .perform(get("/api/product-modules?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].pathName").exists());
    long n = stats.getPrepareStatementCount();
    // ≥4 guards against stats-off false green; ≤6 caps the ancestor batching budget (陷阱 G/J).
    assertTrue(n >= 4L && n <= 6L, "expected 4..6 statements, got " + n);
  }
}
