/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.10 startup migration: legacy v0.0.9 Stories that hang off Requirement directly are
 * re-parented to a per-Requirement "default Sprint", and the {@code sprint_id} column is upgraded
 * from nullable (Hibernate ADD COLUMN default) to {@code NOT NULL} at the DB layer.
 *
 * <p>Steps executed once per JVM start, ordered HIGHEST_PRECEDENCE so they run before any other
 * CommandLineRunner that might INSERT into Story / Sprint:
 *
 * <ol>
 *   <li><b>Step 0</b> — loosen the v0.0.9 {@code rainier_story.requirement_id NOT NULL} constraint
 *       to NULL so the new code path (which omits the column on INSERT) can succeed. Idempotent via
 *       {@code INFORMATION_SCHEMA} check.
 *   <li><b>Step 1</b> — find all Story rows where {@code sprint_id IS NULL}; group by their legacy
 *       {@code requirement_id}; for each group create a default Sprint ({@code
 *       code="SPRINT-DEFAULT-<reqCode>", name="默认 Sprint", status="ACTIVE",
 *       owner=requirement.owner_user_id}) and bulk-UPDATE the Story sprint_id to the new id.
 *   <li><b>Step 2</b> — if Step 1 migrated ≥ 1 row, execute {@code ALTER TABLE rainier_story MODIFY
 *       COLUMN sprint_id BIGINT NOT NULL} so the DB enforces the invariant.
 * </ol>
 *
 * <p>Subsequent boots find no NULL sprint_id rows and exit early without ALTER — no-op.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LegacyStoryToSprintMigration implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(LegacyStoryToSprintMigration.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    if (!legacyRequirementIdColumnExists()) {
      // Fresh install (no v0.0.9 legacy column) — nothing to migrate. The new Hibernate-managed
      // schema already has sprint_id; @Column(nullable=false) is enforced at the application
      // layer and (in fresh installs) Hibernate adds the column as NOT NULL directly. No ALTER
      // needed in this path.
      return;
    }
    relaxRequirementIdColumn();
    int[] counts = migrateStoriesToDefaultSprints();
    int sprintsCreated = counts[0];
    int storiesMigrated = counts[1];
    // v0.0.10 Phase 5 Code-H2 fix: ALTER sprint_id → NOT NULL unconditionally when DB shows it
    // nullable. The old `if (storiesMigrated > 0)` gate left the DB invariant un-enforced after a
    // partial-success first boot (data filled, ALTER never ran).
    boolean sprintIdNowNotNull = upgradeSprintIdToNotNullIfStillNullable();
    if (storiesMigrated > 0 || sprintIdNowNotNull) {
      log.info(
          "LegacyStoryToSprintMigration: created {} default sprints, migrated {} stories;"
              + " sprint_id column upgraded to NOT NULL",
          sprintsCreated,
          storiesMigrated);
    }
  }

  /**
   * Returns true iff this call actually executed the ALTER (sprint_id was nullable). Idempotent:
   * subsequent calls find NOT NULL and short-circuit. Decouples the ALTER from migration counts so
   * a partial-success first boot does not leave the DB invariant un-enforced.
   */
  @SuppressWarnings("unchecked")
  private boolean upgradeSprintIdToNotNullIfStillNullable() {
    List<Object> rows =
        em.createNativeQuery(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS"
                    + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                    + " AND LOWER(COLUMN_NAME) = 'sprint_id'")
            .getResultList();
    if (rows.isEmpty()) {
      return false;
    }
    if ("NO".equalsIgnoreCase(String.valueOf(rows.get(0)))) {
      return false;
    }
    try {
      em.createNativeQuery("ALTER TABLE rainier_story MODIFY COLUMN sprint_id BIGINT NOT NULL")
          .executeUpdate();
    } catch (Exception primaryFailure) {
      try {
        em.createNativeQuery("ALTER TABLE rainier_story ALTER COLUMN sprint_id SET NOT NULL")
            .executeUpdate();
      } catch (Exception fallbackFailure) {
        fallbackFailure.addSuppressed(primaryFailure);
        throw fallbackFailure;
      }
    }
    return true;
  }

  /** Returns true iff {@code rainier_story.requirement_id} column exists (v0.0.9 legacy). */
  @SuppressWarnings("unchecked")
  private boolean legacyRequirementIdColumnExists() {
    List<Object> rows =
        em.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                    + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                    + " AND LOWER(COLUMN_NAME) = 'requirement_id'")
            .getResultList();
    return !rows.isEmpty();
  }

  /**
   * Step 0 — relax v0.0.9 {@code rainier_story.requirement_id NOT NULL} so v0.0.10 INSERTs that
   * omit the legacy column succeed. Idempotent — checks INFORMATION_SCHEMA first.
   */
  private void relaxRequirementIdColumn() {
    @SuppressWarnings("unchecked")
    List<Object> rows =
        em.createNativeQuery(
                "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS"
                    + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                    + " AND LOWER(COLUMN_NAME) = 'requirement_id'")
            .getResultList();
    if (rows.isEmpty()) {
      // Column doesn't exist (e.g. fresh install) — nothing to relax.
      return;
    }
    String nullable = String.valueOf(rows.get(0));
    if ("YES".equalsIgnoreCase(nullable)) {
      return;
    }
    try {
      em.createNativeQuery("ALTER TABLE rainier_story MODIFY COLUMN requirement_id BIGINT NULL")
          .executeUpdate();
    } catch (Exception ex) {
      em.createNativeQuery("ALTER TABLE rainier_story ALTER COLUMN requirement_id SET NULL")
          .executeUpdate();
    }
    log.info("relaxed rainier_story.requirement_id from NOT NULL to NULL (v0.0.10 legacy column)");
  }

  /**
   * Step 1 — find Story rows with NULL sprint_id, group by their legacy requirement_id, create one
   * default Sprint per group, and bulk-UPDATE the Story sprint_id to the new id. Returns {@code
   * [sprintsCreated, storiesMigrated]}.
   */
  @SuppressWarnings("unchecked")
  private int[] migrateStoriesToDefaultSprints() {
    // Orphan if sprint_id is NULL, 0 (MySQL auto-default when ADD COLUMN NOT NULL was applied to
    // existing rows), or points to a row that no longer exists / is soft-deleted.
    List<Object[]> orphans =
        em.createNativeQuery(
                "SELECT s.id, s.requirement_id"
                    + " FROM rainier_story s"
                    + " WHERE s.del_flag = 0"
                    + " AND (s.sprint_id IS NULL"
                    + "      OR s.sprint_id = 0"
                    + "      OR s.sprint_id NOT IN"
                    + "         (SELECT id FROM rainier_sprint WHERE del_flag = 0))")
            .getResultList();
    if (orphans.isEmpty()) {
      return new int[] {0, 0};
    }
    // Group story ids by requirement id.
    Map<Long, List<Long>> byRequirement = new HashMap<>();
    for (Object[] row : orphans) {
      Long storyId = ((Number) row[0]).longValue();
      Long reqId = row[1] == null ? null : ((Number) row[1]).longValue();
      if (reqId == null) {
        log.warn("legacy story id={} has NULL requirement_id; cannot migrate, skipping", storyId);
        continue;
      }
      byRequirement.computeIfAbsent(reqId, k -> new ArrayList<>()).add(storyId);
    }
    int sprintsCreated = 0;
    int storiesMigrated = 0;
    for (Map.Entry<Long, List<Long>> entry : byRequirement.entrySet()) {
      Long reqId = entry.getKey();
      List<Long> storyIds = entry.getValue();
      // Look up the Requirement to get its code and owner.
      List<Object[]> reqRows =
          em.createNativeQuery("SELECT code, owner_user_id FROM rainier_requirement WHERE id = ?")
              .setParameter(1, reqId)
              .getResultList();
      if (reqRows.isEmpty()) {
        log.warn(
            "legacy stories {} reference requirement id={} which no longer exists; skipping",
            storyIds,
            reqId);
        continue;
      }
      String reqCode = (String) reqRows.get(0)[0];
      Long ownerUserId = ((Number) reqRows.get(0)[1]).longValue();
      Long newSprintId = createDefaultSprint(reqId, reqCode, ownerUserId);
      sprintsCreated++;
      // Bulk update all stories in this group.
      Query update =
          em.createNativeQuery("UPDATE rainier_story SET sprint_id = :sid WHERE id IN (:ids)");
      update.setParameter("sid", newSprintId);
      update.setParameter("ids", storyIds);
      int updated = update.executeUpdate();
      storiesMigrated += updated;
      log.info(
          "legacy story migrated to default sprint: requirement_id={} → sprint_id={},"
              + " {} stories",
          reqId,
          newSprintId,
          updated);
    }
    return new int[] {sprintsCreated, storiesMigrated};
  }

  private Long createDefaultSprint(Long requirementId, String reqCode, Long ownerUserId) {
    String code = "SPRINT-DEFAULT-" + reqCode;
    em.createNativeQuery(
            "INSERT INTO rainier_sprint"
                + " (code, name, description, goal, status, requirement_id, owner_user_id,"
                + " start_date, end_date,"
                + " create_by, create_time, update_by, update_time, del_flag)"
                + " VALUES (?, '默认 Sprint', NULL, NULL, 'ACTIVE', ?, ?, NULL, NULL,"
                + " 'system', CURRENT_TIMESTAMP(6), 'system', CURRENT_TIMESTAMP(6), 0)")
        .setParameter(1, code)
        .setParameter(2, requirementId)
        .setParameter(3, ownerUserId)
        .executeUpdate();
    // v0.0.10 Phase 5 Code-H1 fix: scope to live rows + last-inserted to avoid NonUniqueResult if a
    // prior soft-deleted SPRINT-DEFAULT-<code> exists (service-level uniqueness only).
    Number id =
        (Number)
            em.createNativeQuery(
                    "SELECT id FROM rainier_sprint"
                        + " WHERE code = ? AND del_flag = 0"
                        + " ORDER BY id DESC")
                .setParameter(1, code)
                .setMaxResults(1)
                .getSingleResult();
    return id.longValue();
  }
}
