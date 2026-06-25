/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.persistence;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Provides the current auditor username for JPA Auditing (BaseEntity.createdBy / updatedBy).
 *
 * <p>Reads the {@code username} request attribute populated by {@code SecurityFilter} when a valid
 * Bearer JWT is presented. Falls back to {@code "system"} when no request context is bound (CLI,
 * tests, background tasks, Flyway migrations).
 */
@Component("auditorAware")
public class AuditorAwareImpl implements AuditorAware<String> {

  static final String SYSTEM = "system";
  static final String REQ_ATTR_USERNAME = "username";

  /**
   * v0.0.64 — also accept the canonical {@code rainier.username} attribute set by SecurityFilter (the
   * legacy {@code "username"} key was never populated by any filter; this fix lets
   * BaseEntity.createBy/updateBy + project_member.joined_by carry real login names).
   */
  static final String REQ_ATTR_USERNAME_CANONICAL = "rainier.username";

  @Override
  public Optional<String> getCurrentAuditor() {
    HttpServletRequest req = currentRequest();
    if (req == null) {
      return Optional.of(SYSTEM);
    }
    Object v = req.getAttribute(REQ_ATTR_USERNAME_CANONICAL);
    if (v == null) {
      v = req.getAttribute(REQ_ATTR_USERNAME);
    }
    return v == null ? Optional.of(SYSTEM) : Optional.of(v.toString());
  }

  private static HttpServletRequest currentRequest() {
    Object attrs = RequestContextHolder.getRequestAttributes();
    return attrs instanceof ServletRequestAttributes
        ? ((ServletRequestAttributes) attrs).getRequest()
        : null;
  }
}
