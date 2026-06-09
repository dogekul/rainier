/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.bootstrap;

import java.util.List;
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
 * v0.0.10.1 cleanup runner — DROPs the v0.0.9 legacy {@code rainier_story.requirement_id} column
 * after {@link LegacyStoryToSprintMigration} has rewired every Story row through Sprint. Runs AFTER
 * the migration ({@code HIGHEST_PRECEDENCE + 2} vs migration's {@code + 1}) so it can assume the
 * migration's Step 0 column-existence probe has already executed.
 *
 * <p>Idempotent: second boot finds the column already absent and logs a no-op line.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class LegacyRequirementIdColumnCleanup implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(LegacyRequirementIdColumnCleanup.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    if (!legacyColumnExists()) {
      log.info("LegacyRequirementIdColumnCleanup: no-op — requirement_id column already absent");
      return;
    }
    em.createNativeQuery("ALTER TABLE rainier_story DROP COLUMN requirement_id").executeUpdate();
    log.info(
        "LegacyRequirementIdColumnCleanup: dropped legacy requirement_id column from"
            + " rainier_story");
  }

  /** Returns true iff {@code rainier_story.requirement_id} column exists in the live DB schema. */
  @SuppressWarnings("unchecked")
  private boolean legacyColumnExists() {
    List<Object> rows =
        em.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                    + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                    + " AND LOWER(COLUMN_NAME) = 'requirement_id'")
            .getResultList();
    return !rows.isEmpty();
  }
}
