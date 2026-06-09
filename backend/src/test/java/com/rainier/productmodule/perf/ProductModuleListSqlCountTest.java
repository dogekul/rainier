/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.perf;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** TC-PERF-PMOD-001: 2 page + 1 user + 1 product = 4. */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleListSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private ProductModuleRepository repo;
  @Autowired private ProductRepository productRepo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    repo.deleteAll();
    productRepo.deleteAll();
    categoryRepo.deleteAll();
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
    ProductCategory c = new ProductCategory();
    c.setCode("CAT-MOD-PERF");
    c.setName("x");
    c.setStatus(ProductCategoryStatus.ACTIVE);
    c.setOwnerUserId(uids[0]);
    Long cid = categoryRepo.saveAndFlush(c).getId();
    Long[] pids = new Long[4];
    for (int i = 0; i < 4; i++) {
      Product p = new Product();
      p.setCode("PROD-MOD-PERF-" + i);
      p.setName("x");
      p.setStatus(ProductStatus.ACTIVE);
      p.setCategoryId(cid);
      p.setOwnerUserId(uids[i % 4]);
      pids[i] = productRepo.saveAndFlush(p).getId();
    }
    for (int i = 0; i < 20; i++) {
      ProductModule m = new ProductModule();
      m.setCode("MOD-PERF-" + i);
      m.setName("M " + i);
      m.setStatus(ProductModuleStatus.PLANNING);
      m.setProductId(pids[i % 4]);
      m.setOwnerUserId(uids[i % 4]);
      repo.saveAndFlush(m);
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
        .andExpect(jsonPath("$.content[0].productName").exists());
    long n = stats.getPrepareStatementCount();
    assertTrue(n >= 4L && n <= 5L, "expected 4..5 statements, got " + n);
  }
}
