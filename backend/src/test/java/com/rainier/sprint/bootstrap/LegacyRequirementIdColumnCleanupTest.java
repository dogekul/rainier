/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link LegacyRequirementIdColumnCleanup}. Covers TC-CLN-001 (first-boot DROP),
 * TC-CLN-002 (idempotent no-op), TC-CLN-003 (@Order ordering vs migration).
 */
@SpringBootTest
@ActiveProfiles("test")
class LegacyRequirementIdColumnCleanupTest {

  @Autowired private LegacyRequirementIdColumnCleanup cleanup;
  @PersistenceContext private EntityManager em;

  private ListAppender<ILoggingEvent> logCaptor;
  private Logger cleanupLogger;

  @BeforeEach
  @Transactional
  void setUp() {
    // Drop any column lingering from a previous test method.
    try {
      em.createNativeQuery("ALTER TABLE rainier_story DROP COLUMN requirement_id").executeUpdate();
    } catch (Exception ignored) {
      // Column may not exist on fresh H2 — ignore.
    }
    cleanupLogger = (Logger) LoggerFactory.getLogger(LegacyRequirementIdColumnCleanup.class);
    logCaptor = new ListAppender<>();
    logCaptor.start();
    cleanupLogger.addAppender(logCaptor);
  }

  @AfterEach
  void tearDown() {
    if (cleanupLogger != null && logCaptor != null) {
      cleanupLogger.detachAppender(logCaptor);
      logCaptor.stop();
    }
  }

  /** TC-CLN-001: 首次启动 DROP 遗留列 + 输出 INFO 日志. */
  @Test
  @Transactional
  void run_columnPresent_dropsItAndLogsInfo() {
    em.createNativeQuery("ALTER TABLE rainier_story ADD COLUMN requirement_id BIGINT")
        .executeUpdate();
    em.flush();
    em.clear();

    cleanup.run();

    long colCount =
        ((Number)
                em.createNativeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                            + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                            + " AND LOWER(COLUMN_NAME) = 'requirement_id'")
                    .getSingleResult())
            .longValue();
    assertEquals(0L, colCount, "requirement_id column should be dropped");
    // Test-L2 mutation-kill: assert the runner dropped ONLY the legacy column — a typo like
    // "DROP COLUMN sprint_id" would still pass colCount==0 but explode the schema.
    long sprintIdCount =
        ((Number)
                em.createNativeQuery(
                        "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                            + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                            + " AND LOWER(COLUMN_NAME) = 'sprint_id'")
                    .getSingleResult())
            .longValue();
    assertEquals(1L, sprintIdCount, "sprint_id column must NOT be dropped — wrong-column mutation");
    assertTrue(
        logCaptor.list.stream()
            .anyMatch(
                e ->
                    e.getLevel() == Level.INFO
                        && e.getFormattedMessage()
                            .contains(
                                "LegacyRequirementIdColumnCleanup: dropped legacy requirement_id"
                                    + " column from rainier_story")),
        "expected INFO log with canonical 'dropped legacy' wording");
  }

  /** TC-CLN-002: 二次启动 no-op + 输出 no-op 日志. */
  @Test
  @Transactional
  void run_columnAbsent_isNoOpAndLogsNoOp() {
    // setUp already dropped the column; column should not exist now.
    cleanup.run();

    assertTrue(
        logCaptor.list.stream()
            .anyMatch(
                e ->
                    e.getLevel() == Level.INFO
                        && e.getFormattedMessage()
                            .contains(
                                "LegacyRequirementIdColumnCleanup: no-op — requirement_id column"
                                    + " already absent")),
        "expected INFO log with canonical no-op wording");
    assertTrue(
        logCaptor.list.stream().noneMatch(e -> e.getFormattedMessage().contains("dropped legacy")),
        "no-op run must not emit 'dropped legacy' log line");
  }

  /**
   * TC-CLN-003: runner 链式顺序：cleanup @Order(+2) > migration @Order(+1) > base HIGHEST_PRECEDENCE.
   */
  @Test
  void orderAnnotations_cleanupRunsAfterMigration() {
    Order cleanupOrder =
        AnnotationUtils.findAnnotation(LegacyRequirementIdColumnCleanup.class, Order.class);
    Order migrationOrder =
        AnnotationUtils.findAnnotation(LegacyStoryToSprintMigration.class, Order.class);
    assertNotNull(cleanupOrder, "@Order missing on LegacyRequirementIdColumnCleanup");
    assertNotNull(migrationOrder, "@Order missing on LegacyStoryToSprintMigration");
    assertEquals(Ordered.HIGHEST_PRECEDENCE + 2, cleanupOrder.value());
    assertEquals(Ordered.HIGHEST_PRECEDENCE + 1, migrationOrder.value());
    assertTrue(cleanupOrder.value() > migrationOrder.value(), "cleanup must run after migration");
  }
}
