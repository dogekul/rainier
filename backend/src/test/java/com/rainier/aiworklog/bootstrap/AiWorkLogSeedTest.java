/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** v0.0.43 seed-driven flywheel shell (flag on). Covers TC-AIW-SEED-001..002. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.demo.ai-work-log-seed.enabled=true")
class AiWorkLogSeedTest {

  @Autowired private AiWorkLogSeed seed;
  @Autowired private AiWorkLogRepository repo;

  /** TC-AIW-SEED-001: seeds PROPOSED (evidence-bearing) rows when the table is empty. */
  @Test
  void seedsWhenEmpty() {
    repo.deleteAll();
    seed.run();
    assertTrue(repo.count() >= 1, "expected seeded rows");
    repo.findAll()
        .forEach(
            a -> {
              assertEquals(AiWorkLogStatus.PROPOSED, a.getStatus());
              assertNotNull(a.getEvidence());
              assertFalse(a.getEvidence().trim().isEmpty(), "seeded evidence must be non-blank");
            });
  }

  /** TC-AIW-SEED-002: idempotent — does not re-seed when the table is non-empty. */
  @Test
  void idempotent() {
    repo.deleteAll();
    seed.run();
    long after = repo.count();
    seed.run();
    assertEquals(after, repo.count(), "second run must not add rows");
  }
}
