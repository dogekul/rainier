/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.capability.domain.CapabilityTag;
import com.rainier.capability.repository.CapabilityTagRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** v0.0.85 (C5) — covers TC-CAP-SEED-001..002. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.demo.capability-seed.enabled=true")
class CapabilityTagSeedTest {

  @Autowired private CapabilityTagSeed seed;
  @Autowired private CapabilityTagRepository repo;

  /** TC-CAP-SEED-001: empty table → at least 10 rows, all three categories present. */
  @Test
  void seedsWhenEmpty() {
    repo.deleteAll();
    seed.run();
    assertTrue(repo.count() >= 10, "expected >= 10 seeded tags, got " + repo.count());
    Set<String> cats = new HashSet<>();
    for (CapabilityTag t : repo.findAll()) {
      cats.add(t.getCategory());
    }
    assertTrue(cats.contains("TECH"));
    assertTrue(cats.contains("PRODUCT"));
    assertTrue(cats.contains("SOFT"));
  }

  /** TC-CAP-SEED-002: idempotent — second run does not insert duplicates. */
  @Test
  void idempotent() {
    repo.deleteAll();
    seed.run();
    long after = repo.count();
    seed.run();
    assertEquals(after, repo.count(), "second run must not add rows");
  }
}
