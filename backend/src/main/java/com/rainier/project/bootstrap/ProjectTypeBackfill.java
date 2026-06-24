/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.bootstrap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup self-heal — backfill {@code rainier_project.project_type} to {@code CASUAL} for any legacy
 * row left NULL.
 *
 * <p>v0.0.16 adds {@code project_type} as a <b>nullable</b> column so the {@code ddl-auto=update}
 * ALTER is safe on existing MySQL rows (a {@code NOT NULL} ALTER would fail under strict mode). This
 * runner + the {@code ProjectDetail.from} null→CASUAL coalesce form a two-layer default: the runner
 * fixes the stored data once at boot; the DTO coalesce defends any read that races the backfill.
 *
 * <p>Native UPDATE (not JPA) intentionally bypasses {@code @SQLDelete} — same escape-hatch rationale
 * as {@link DanglingProjectIdCleanup}. Idempotent: subsequent boots match zero rows.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProjectTypeBackfill implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ProjectTypeBackfill.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    int updated =
        em.createNativeQuery(
                "UPDATE rainier_project SET project_type = 'CASUAL' WHERE project_type IS NULL")
            .executeUpdate();
    if (updated > 0) {
      log.warn("ProjectTypeBackfill: backfilled {} rainier_project rows project_type → CASUAL", updated);
    }
    // v0.0.48 — 正式拆分：退役的 FORMAL 迁移到 主业-功能建设 (CORE_FEATURE)。幂等：后续启动匹配 0 行。
    int migrated =
        em.createNativeQuery(
                "UPDATE rainier_project SET project_type = 'CORE_FEATURE' WHERE project_type = 'FORMAL'")
            .executeUpdate();
    if (migrated > 0) {
      log.warn("ProjectTypeBackfill: migrated {} rainier_project rows FORMAL → CORE_FEATURE", migrated);
    }
  }
}
