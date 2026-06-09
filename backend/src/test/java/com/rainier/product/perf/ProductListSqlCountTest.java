/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.perf;

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

/** TC-PERF-PROD-001: 2 page + 1 user + 1 category = 4. */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductListSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private ProductRepository repo;
  @Autowired private ProductCategoryRepository categoryRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    repo.deleteAll();
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
    Long[] cids = new Long[4];
    for (int i = 0; i < 4; i++) {
      ProductCategory c = new ProductCategory();
      c.setCode("CAT-PROD-PERF-" + i);
      c.setName("Cat " + i);
      c.setStatus(ProductCategoryStatus.ACTIVE);
      c.setOwnerUserId(uids[i % 4]);
      cids[i] = categoryRepo.saveAndFlush(c).getId();
    }
    for (int i = 0; i < 20; i++) {
      Product p = new Product();
      p.setCode("PROD-PERF-" + i);
      p.setName("P " + i);
      p.setStatus(ProductStatus.PLANNING);
      p.setCategoryId(cids[i % 4]);
      p.setOwnerUserId(uids[i % 4]);
      repo.saveAndFlush(p);
    }
  }

  @Test
  void list_size20_executesBoundedSqlCount() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "stats must be enabled");
    stats.clear();
    mockMvc
        .perform(get("/api/products?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].categoryName").exists());
    long n = stats.getPrepareStatementCount();
    assertTrue(n >= 4L && n <= 5L, "expected 4..5 statements, got " + n);
  }
}
