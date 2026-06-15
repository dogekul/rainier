/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.bootstrap;

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
 * Startup migration — remap legacy {@code rainier_requirement.status} values to the v0.0.19 set
 * (草稿/审批中/分析中/实施中/已交付/已关闭), preserving each requirement's meaning:
 *
 * <ul>
 *   <li>IN_REVIEW → IN_APPROVAL
 *   <li>APPROVED → IN_ANALYSIS
 *   <li>IN_DEV → IN_PROGRESS
 *   <li>DEPRECATED → CLOSED
 * </ul>
 *
 * <p>DRAFT / DELIVERED are unchanged. Native UPDATE (bypasses {@code @SQLDelete}) — same escape-hatch
 * pattern as {@code ProjectTypeBackfill}. Idempotent: subsequent boots match zero rows.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequirementStatusBackfill implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(RequirementStatusBackfill.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    int n = 0;
    n += remap("IN_REVIEW", "IN_APPROVAL");
    n += remap("APPROVED", "IN_ANALYSIS");
    n += remap("IN_DEV", "IN_PROGRESS");
    n += remap("DEPRECATED", "CLOSED");
    if (n > 0) {
      log.warn("RequirementStatusBackfill: remapped {} legacy requirement status rows", n);
    }
  }

  private int remap(String oldStatus, String newStatus) {
    return em.createNativeQuery(
            "UPDATE rainier_requirement SET status = :newStatus WHERE status = :oldStatus")
        .setParameter("newStatus", newStatus)
        .setParameter("oldStatus", oldStatus)
        .executeUpdate();
  }
}
