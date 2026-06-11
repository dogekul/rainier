/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.perf;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.common.domain.Priority;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
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
 * TC-PERF-SPR-PF-001 (v0.0.14): GET /api/sprints?size=20 with product-bound sprints stays batched.
 * Baseline was exactly 6 (2 page + user/req/project batch + storyCount); product enrich adds one
 * batch → ≤ 7. Range assert (≥4 ∧ ≤7) guards both stats-off false green and per-row regressions.
 */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintProductEnrichSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  @Transactional
  void seed() {
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
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
    Project proj = new Project();
    proj.setCode("PROJ-PF");
    proj.setName("Apollo");
    proj.setStatus(ProjectStatus.ACTIVE);
    proj.setOwnerUserId(uids[0]);
    proj.setEnabled(true);
    Long projId = projectRepo.saveAndFlush(proj).getId();
    Long[] prodIds = new Long[3];
    for (int i = 0; i < 3; i++) {
      Product p = new Product();
      p.setCode("PROD-PF-" + i);
      p.setName("产品 " + i);
      p.setStatus(ProductStatus.ACTIVE);
      p.setOwnerUserId(uids[0]);
      prodIds[i] = productRepo.saveAndFlush(p).getId();
    }
    for (int i = 0; i < 20; i++) {
      Requirement r = new Requirement();
      r.setCode("REQ-PF-" + i);
      r.setTitle("需求 " + i);
      r.setOwnerUserId(uids[i % 4]);
      r.setProjectId(projId);
      r.setStatus(RequirementStatus.DRAFT);
      r.setPriority(Priority.MEDIUM);
      Long reqId = requirementRepo.saveAndFlush(r).getId();
      Sprint s = new Sprint();
      s.setCode("SPR-PF-" + i);
      s.setName("迭代 " + i);
      s.setStatus(SprintStatus.PLANNING);
      s.setRequirementId(reqId);
      s.setProductId(prodIds[i % 3]); // product-bound → product batch fires
      s.setOwnerUserId(uids[i % 4]);
      sprintRepo.saveAndFlush(s);
    }
  }

  @Test
  void list_size20_withProduct_executesBoundedSqlCount() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "stats must be enabled");
    stats.clear();
    mockMvc
        .perform(get("/api/sprints?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20))
        .andExpect(jsonPath("$.content[0].productName").exists());
    long n = stats.getPrepareStatementCount();
    assertTrue(n >= 4L && n <= 7L, "expected 4..7 statements, got " + n);
  }
}
