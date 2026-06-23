/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.bootstrap;

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
 * Startup backfill (v0.0.47) — seed {@code rainier_opportunity.stage_entered_at} for rows that predate
 * the column. Going forward the service sets it on create + on each advance; legacy rows have NULL, so
 * fall back to {@code update_time} (a sensible "in this stage since" lower bound for the 停留时长 signal).
 *
 * <p>Non-destructive: touches ONLY the new column, ONLY where it is NULL — no existing business field
 * (客户/阶段/金额/状态) is modified. Native UPDATE (same escape-hatch pattern as {@code ProjectTypeBackfill} /
 * {@code RequirementStatusBackfill}), idempotent — subsequent boots match zero rows.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OpportunityStageEnteredAtBackfill implements CommandLineRunner {

  private static final Logger log =
      LoggerFactory.getLogger(OpportunityStageEnteredAtBackfill.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    int n =
        em.createNativeQuery(
                "UPDATE rainier_opportunity SET stage_entered_at = update_time "
                    + "WHERE stage_entered_at IS NULL")
            .executeUpdate();
    if (n > 0) {
      log.warn("OpportunityStageEnteredAtBackfill: backfilled {} rows (stage_entered_at=update_time)", n);
    }
  }
}
