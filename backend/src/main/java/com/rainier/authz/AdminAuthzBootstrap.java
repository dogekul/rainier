/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstraps the first admin so enabling admin authz can't lock everyone out. Once {@code
 * admin-authz.enabled} is on, granting the first admin via {@code PUT /api/roles} would itself require
 * admin — a deadlock. On startup, if NO role has {@code adminAccess=true}, this elevates the role
 * whose code is {@code app.security.bootstrap-admin-role} (default {@code PMO}).
 *
 * <p>Idempotent: a no-op once any admin role exists. Also self-heals a total lockout (e.g. someone
 * demoted every role). Gated on {@code admin-authz.enabled} so it does nothing in the test profile.
 *
 * <p>Native UPDATE (like {@code ProjectTypeBackfill}) intentionally bypasses {@code @SQLDelete}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AdminAuthzBootstrap implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminAuthzBootstrap.class);

  private final boolean enabled;
  private final String bootstrapRoleCode;

  @PersistenceContext private EntityManager em;

  public AdminAuthzBootstrap(
      @Value("${app.security.admin-authz.enabled:true}") boolean enabled,
      @Value("${app.security.bootstrap-admin-role:PMO}") String bootstrapRoleCode) {
    this.enabled = enabled;
    this.bootstrapRoleCode = bootstrapRoleCode;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) {
      return;
    }
    Number adminCount =
        (Number)
            em.createNativeQuery(
                    "SELECT COUNT(*) FROM rainier_role WHERE admin_access = TRUE AND del_flag = 0")
                .getSingleResult();
    if (adminCount.longValue() > 0) {
      return; // an admin role already exists — nothing to bootstrap
    }
    int updated =
        em.createNativeQuery(
                "UPDATE rainier_role SET admin_access = TRUE WHERE code = ? AND del_flag = 0")
            .setParameter(1, bootstrapRoleCode)
            .executeUpdate();
    if (updated > 0) {
      log.warn(
          "AdminAuthzBootstrap: no admin role found — elevated role '{}' to admin (first-admin bootstrap)",
          bootstrapRoleCode);
    } else {
      log.warn(
          "AdminAuthzBootstrap: no admin role and no '{}' role to elevate — grant adminAccess manually",
          bootstrapRoleCode);
    }
  }
}
