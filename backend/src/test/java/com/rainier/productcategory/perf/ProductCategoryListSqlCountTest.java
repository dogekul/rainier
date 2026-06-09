/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.perf;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** TC-PERF-PCAT-001: list size=20 enrich → 2 page + 1 user batch = 3 (range ≤5 ∧ ≥3). */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCategoryListSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private ProductCategoryRepository repo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    repo.deleteAll();
    userRepo.deleteAll();
    Long[] uids = new Long[4];
    for (int i = 0; i < 4; i++) {
      User u = new User();
      u.setLoginName("u" + i);
      u.setName("User " + i);
      u.setIsInternal(true);
      u.setEnabled(true);
      uids[i] = userRepo.saveAndFlush(u).getId();
    }
    for (int i = 0; i < 20; i++) {
      ProductCategory c = new ProductCategory();
      c.setCode("CAT-PERF-" + i);
      c.setName("Cat " + i);
      c.setStatus(ProductCategoryStatus.ACTIVE);
      c.setOwnerUserId(uids[i % 4]);
      repo.saveAndFlush(c);
    }
  }

  @Test
  void list_size20_executesBoundedSqlCount() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "hibernate.generate_statistics must be true");
    stats.clear();
    mockMvc
        .perform(get("/api/product-categories?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].ownerName").exists());
    long n = stats.getPrepareStatementCount();
    // 2 page (data + count) + 1 user batch = 3; allow ≤ 5 headroom.
    assertTrue(n >= 3L && n <= 5L, "expected 3..5 statements, got " + n);
  }
}
