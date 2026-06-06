/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.bootstrap;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Startup self-heal — NULL out any {@code project_id} that no longer references an existing Project
 * row.
 *
 * <p>Activated by v0.0.8 to enforce the "strict reads" decision (design.md decision 6): once this
 * runs at startup, the service-layer enrich code can assume {@code projectId != null} implies a
 * resolvable Project. v0.0.7 left exactly one such dangling row ({@code rainier_user_role.id=2
 * project_id=42}); the first v0.0.8 boot cleans it. Subsequent boots are no-ops on a clean DB.
 *
 * <p>Uses native SQL UPDATE to bypass {@code @SQLDelete} (which would set del_flag=1 instead of the
 * column to NULL) — this is the single intentional escape hatch from the soft-delete lifecycle in
 * the codebase.
 */
@Component
public class DanglingProjectIdCleanup implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(DanglingProjectIdCleanup.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    cleanDanglingForTable("rainier_requirement", "project_id");
    cleanDanglingForTable("rainier_user_role", "project_id");
  }

  /**
   * For one table+column, NULL out values that don't resolve to a live Project row. Logs a WARN per
   * row affected so an admin reading docker logs can see what was cleaned.
   */
  private void cleanDanglingForTable(String table, String column) {
    String selectSql =
        "SELECT id, "
            + column
            + " FROM "
            + table
            + " WHERE "
            + column
            + " IS NOT NULL"
            + " AND "
            + column
            + " NOT IN (SELECT id FROM rainier_project WHERE del_flag = 0)";
    @SuppressWarnings("unchecked")
    java.util.List<Object[]> dangling = em.createNativeQuery(selectSql).getResultList();
    if (dangling.isEmpty()) {
      return;
    }
    String updateSql =
        "UPDATE "
            + table
            + " SET "
            + column
            + " = NULL"
            + " WHERE "
            + column
            + " IS NOT NULL"
            + " AND "
            + column
            + " NOT IN (SELECT id FROM rainier_project WHERE del_flag = 0)";
    int updated = em.createNativeQuery(updateSql).executeUpdate();
    for (Object[] row : dangling) {
      log.warn("cleaned dangling project_id from {}.{} (was project_id={})", table, row[0], row[1]);
    }
    log.warn("DanglingProjectIdCleanup: cleaned {} rows from {}.{}", updated, table, column);
  }
}
