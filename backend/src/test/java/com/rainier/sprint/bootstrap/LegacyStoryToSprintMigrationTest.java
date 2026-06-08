/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tests for {@link LegacyStoryToSprintMigration}. Covers TC-SPR-MIG-001 (first-boot migration +
 * column upgrade) and TC-SPR-MIG-002 (idempotency on second run).
 */
@SpringBootTest
@ActiveProfiles("test")
class LegacyStoryToSprintMigrationTest {

  @Autowired private LegacyStoryToSprintMigration migration;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @PersistenceContext private EntityManager em;

  private ListAppender<ILoggingEvent> logCaptor;
  private Logger migrationLogger;

  @BeforeEach
  @Transactional
  void setUp() {
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    try {
      em.createNativeQuery("ALTER TABLE rainier_story DROP COLUMN requirement_id").executeUpdate();
    } catch (Exception ignored) {
      // Column may not exist on fresh H2 — ignore.
    }
    migrationLogger = (Logger) LoggerFactory.getLogger(LegacyStoryToSprintMigration.class);
    logCaptor = new ListAppender<>();
    logCaptor.start();
    migrationLogger.addAppender(logCaptor);
  }

  @AfterEach
  void tearDown() {
    if (migrationLogger != null && logCaptor != null) {
      migrationLogger.detachAppender(logCaptor);
      logCaptor.stop();
    }
  }

  private long[] seedReqOwner() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long userId = userRepo.saveAndFlush(u).getId();
    Project p = new Project();
    p.setCode("PROJ-MIG");
    p.setName("x");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(userId);
    p.setEnabled(true);
    Long projectId = projectRepo.saveAndFlush(p).getId();
    Requirement r = new Requirement();
    r.setCode("REQ-MIG");
    r.setTitle("x");
    r.setOwnerUserId(userId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    Long reqId = requirementRepo.saveAndFlush(r).getId();
    return new long[] {userId, reqId};
  }

  /**
   * TC-SPR-MIG-001: 首次启动迁移旧 Story + ALTER 升级 sprint_id 至 NOT NULL + INFO 日志 + DESCRIBE shows
   * Null=NO.
   */
  @Test
  @Transactional
  void run_migratesOrphanStories_andUpgradesSprintIdToNotNull() {
    long[] seed = seedReqOwner();
    Long userId = seed[0];
    Long reqId = seed[1];

    // Simulate v0.0.9 legacy state: add requirement_id column, relax sprint_id to nullable.
    em.createNativeQuery("ALTER TABLE rainier_story ADD COLUMN requirement_id BIGINT")
        .executeUpdate();
    em.createNativeQuery("ALTER TABLE rainier_story ALTER COLUMN sprint_id SET NULL")
        .executeUpdate();
    em.createNativeQuery(
            "INSERT INTO rainier_story"
                + " (code, title, status, priority, owner_user_id, requirement_id, sprint_id,"
                + " create_by, create_time, update_by, update_time, del_flag)"
                + " VALUES (?, 'x', 'DRAFT', 'MEDIUM', ?, ?, NULL,"
                + " 'system', CURRENT_TIMESTAMP(), 'system', CURRENT_TIMESTAMP(), 0)")
        .setParameter(1, "STR-ORPHAN")
        .setParameter(2, userId)
        .setParameter(3, reqId)
        .executeUpdate();
    em.flush();
    em.clear();

    migration.run();

    Long defaultSprintId =
        ((Number)
                em.createNativeQuery(
                        "SELECT id FROM rainier_sprint WHERE code = 'SPRINT-DEFAULT-REQ-MIG'")
                    .getSingleResult())
            .longValue();
    assertNotNull(defaultSprintId);
    Story reloaded =
        storyRepo.findAll().stream()
            .filter(s -> "STR-ORPHAN".equals(s.getCode()))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
    assertEquals(defaultSprintId, reloaded.getSprintId());
    assertEquals(StoryStatus.DRAFT, reloaded.getStatus());

    // v0.0.10 Phase 5 Test-C1 fix: assert DB column upgraded to NOT NULL.
    String nullable =
        (String)
            em.createNativeQuery(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                        + " AND LOWER(COLUMN_NAME) = 'sprint_id'")
                .getSingleResult();
    assertEquals("NO", nullable, "sprint_id column should be NOT NULL after migration");

    // v0.0.10 Phase 5 Test-H2 fix: assert summary INFO log + per-row INFO log are emitted.
    assertTrue(
        logCaptor.list.stream()
            .anyMatch(
                e ->
                    e.getLevel() == Level.INFO
                        && e.getFormattedMessage()
                            .contains("legacy story migrated to default sprint: requirement_id=")),
        "expected per-row INFO migration log");
    assertTrue(
        logCaptor.list.stream()
            .anyMatch(
                e ->
                    e.getLevel() == Level.INFO
                        && e.getFormattedMessage()
                            .contains(
                                "LegacyStoryToSprintMigration: created 1 default sprints,"
                                    + " migrated 1 stories; sprint_id column upgraded to NOT NULL")),
        "expected summary INFO log with canonical wording");
  }

  /**
   * TC-SPR-MIG-002: 二次启动 no-op — no new Sprint, no second ALTER (column stays NOT NULL), actually
   * exercises the full migration path (legacy column present + sprint_id nullable).
   */
  @Test
  @Transactional
  void run_secondInvocation_noOp_whenAllStoriesMigrated() {
    long[] seed = seedReqOwner();
    Long userId = seed[0];
    Long reqId = seed[1];
    em.createNativeQuery("ALTER TABLE rainier_story ADD COLUMN requirement_id BIGINT")
        .executeUpdate();
    em.createNativeQuery("ALTER TABLE rainier_story ALTER COLUMN sprint_id SET NULL")
        .executeUpdate();
    em.createNativeQuery(
            "INSERT INTO rainier_story"
                + " (code, title, status, priority, owner_user_id, requirement_id, sprint_id,"
                + " create_by, create_time, update_by, update_time, del_flag)"
                + " VALUES (?, 'x', 'DRAFT', 'MEDIUM', ?, ?, NULL,"
                + " 'system', CURRENT_TIMESTAMP(), 'system', CURRENT_TIMESTAMP(), 0)")
        .setParameter(1, "STR-IDEMP")
        .setParameter(2, userId)
        .setParameter(3, reqId)
        .executeUpdate();
    em.flush();
    em.clear();

    // First run — should migrate + ALTER.
    migration.run();
    long sprintCountAfterFirst = sprintRepo.count();
    assertEquals(1L, sprintCountAfterFirst);

    // Clear logs to focus on second run.
    logCaptor.list.clear();

    // Second run — should find no orphans, no new sprints, no second ALTER, no summary log.
    migration.run();
    long sprintCountAfterSecond = sprintRepo.count();
    assertEquals(
        sprintCountAfterFirst, sprintCountAfterSecond, "second run must not create new sprints");
    assertTrue(
        logCaptor.list.stream()
            .noneMatch(e -> e.getFormattedMessage().contains("LegacyStoryToSprintMigration:")),
        "second run must not emit migration summary log");
    // Column should still be NOT NULL.
    String nullable =
        (String)
            em.createNativeQuery(
                    "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE LOWER(TABLE_NAME) = 'rainier_story'"
                        + " AND LOWER(COLUMN_NAME) = 'sprint_id'")
                .getSingleResult();
    assertEquals("NO", nullable, "sprint_id must stay NOT NULL across boots");
  }
}
