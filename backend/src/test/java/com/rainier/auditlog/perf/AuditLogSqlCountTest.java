/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.perf;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
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
 * TC-PERF-AUD-001 (audit write adds exactly 1 INSERT to a create path) + TC-PERF-AUD-002
 * (audit list = page + count, no enrich joins). Range asserts guard against stats-off false green.
 */
@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogSqlCountTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManagerFactory emf;
  @Autowired private AuditLogRepository auditRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long uid;

  @BeforeEach
  @Transactional
  void seed() {
    auditRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
  }

  /** TC-PERF-AUD-001: the audit INSERT is the only extra statement on a create path. */
  @Test
  void create_auditAddsAtMostOneInsert() throws Exception {
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.clear();
    long entityInsertsBefore = stats.getEntityInsertCount();
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-PERF");
    body.put("title", "需求");
    body.put("ownerUserId", uid);
    body.put("priority", "MEDIUM");
    mockMvc
        .perform(
            post("/api/requirements")
                .requestAttr("username", "alice")
                .contentType("application/json")
                .content(body.toString()))
        .andExpect(status().isCreated());
    long auditInserts = auditRepo.count();
    // Exactly one audit row is inserted by the create path.
    org.junit.jupiter.api.Assertions.assertEquals(1, auditInserts, "exactly one audit row");
    long totalEntityInserts = stats.getEntityInsertCount() - entityInsertsBefore;
    // Requirement(1) + AuditLog(1) = 2 entity inserts; audit contributes exactly 1.
    assertTrue(totalEntityInserts >= 2 && totalEntityInserts <= 3,
        "expected 2..3 entity inserts (business + audit), got " + totalEntityInserts);
  }

  /** TC-PERF-AUD-002: list = page select + count, no enrichment joins. */
  @Test
  void list_size20_boundedSqlCount() throws Exception {
    for (int i = 0; i < 20; i++) {
      AuditLog a = new AuditLog();
      a.setActor("alice");
      a.setEntityType("REQUIREMENT");
      a.setEntityId((long) i);
      a.setAction(AuditAction.CREATE);
      a.setSummary("CREATE REQUIREMENT#" + i);
      auditRepo.saveAndFlush(a);
    }
    Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
    assertTrue(stats.isStatisticsEnabled(), "stats must be enabled");
    stats.clear();
    mockMvc
        .perform(get("/api/audit-logs?page=0&size=20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(20));
    long n = stats.getPrepareStatementCount();
    assertTrue(n >= 2L && n <= 3L, "expected 2..3 statements (page + count), got " + n);
  }
}
