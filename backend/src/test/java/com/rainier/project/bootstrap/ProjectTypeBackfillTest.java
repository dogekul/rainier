/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * TC-PROJTYPE-009 — startup backfill of legacy NULL {@code project_type} → CASUAL.
 *
 * <p>Mirrors {@link DanglingProjectIdCleanupTest}: non-transactional so {@code saveAndFlush} commits,
 * the runner's own {@code @Transactional} native UPDATE commits, and {@code findById} re-reads fresh.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProjectTypeBackfillTest {

  @Autowired private ProjectTypeBackfill backfill;
  @Autowired private ProjectRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  /** A legacy row left with project_type=NULL is backfilled to CASUAL; other columns untouched. */
  @Test
  void run_backfillsNullToCasual_otherFieldsUnchanged() {
    Project p = new Project();
    p.setCode("BF-1");
    p.setName("Legacy");
    p.setStatus("PLANNING");
    p.setOwnerUserId(1L);
    p.setEnabled(true);
    // projectType intentionally left null — simulates a row that predates v0.0.16.
    Long id = repo.saveAndFlush(p).getId();

    backfill.run();

    Project reloaded = repo.findById(id).orElseThrow(IllegalStateException::new);
    assertEquals("CASUAL", reloaded.getProjectType(), "NULL project_type backfilled to CASUAL");
    // standing constraint — backfill must not touch any other column.
    assertEquals("BF-1", reloaded.getCode());
    assertEquals("Legacy", reloaded.getName());
    assertEquals("PLANNING", reloaded.getStatus());
    assertEquals(1L, reloaded.getOwnerUserId());
    assertTrue(reloaded.getEnabled());
  }
}
