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

  @Override
  public Optional<String> getCurrentAuditor() {
    return Optional.ofNullable(currentRequest())
        .map(req -> req.getAttribute(REQ_ATTR_USERNAME))
        .map(Object::toString)
        .map(Optional::of)
        .orElseGet(() -> Optional.of(SYSTEM));
  }

  private static HttpServletRequest currentRequest() {
    Object attrs = RequestContextHolder.getRequestAttributes();
    return attrs instanceof ServletRequestAttributes
        ? ((ServletRequestAttributes) attrs).getRequest()
        : null;
  }
}
