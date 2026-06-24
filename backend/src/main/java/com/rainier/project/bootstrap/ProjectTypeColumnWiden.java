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
 * Startup self-heal (v0.0.48) — widen {@code rainier_project.project_type} to VARCHAR(32) so the new
 * {@code EXTERNAL_DELIVERY} (17 chars) value fits. Hibernate {@code ddl-auto=update} does NOT alter the
 * length of an existing column, so a MySQL volume created when the column was VARCHAR(16) would reject
 * the insert ("Data too long"). This native ALTER fixes the live column once at boot.
 *
 * <p>Best-effort + idempotent: re-widening to 32 is a no-op on MySQL; on H2 (tests) the schema is
 * recreated from the entity at width 32, so the MySQL-syntax ALTER is unnecessary — any failure is
 * caught and logged, never blocking startup. Runs before {@link ProjectTypeBackfill} (which only
 * writes short values) and before the app serves traffic.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProjectTypeColumnWiden implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ProjectTypeColumnWiden.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    try {
      em.createNativeQuery("ALTER TABLE rainier_project MODIFY COLUMN project_type VARCHAR(32)")
          .executeUpdate();
      log.info("ProjectTypeColumnWiden: ensured rainier_project.project_type is VARCHAR(32)");
    } catch (RuntimeException e) {
      // H2 (tests) uses different ALTER syntax and the column is already 32 there → safe to skip.
      log.warn("ProjectTypeColumnWiden: skipped widen ({})", e.getMessage());
    }
  }
}
