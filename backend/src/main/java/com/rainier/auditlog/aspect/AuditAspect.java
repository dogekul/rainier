/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.aspect;

import com.rainier.auth.RequestUserContext;
import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.service.AuditLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.AuditorAware;
import org.springframework.stereotype.Component;

/**
 * v0.0.15: records one audit row after each successful {@code *Service.create/update/delete}.
 *
 * <ul>
 *   <li>{@code @AfterReturning} only — a throwing business method is NOT audited.
 *   <li>Writes via {@link AuditLogService#record} (named {@code record}, not create/update/delete)
 *       so the aspect never matches itself (no recursion).
 *   <li>Runs inside the business transaction (see {@code AuditTxConfig}); the audit row rolls back
 *       with a failed business commit.
 *   <li>{@code entityType} from the DECLARING class name (avoids CGLIB proxy names); {@code action}
 *       from the method name; {@code entityId} from the returned {@code *Detail.getId()} (create /
 *       update) or the first {@code Long} argument (delete). Reflection failures degrade to a null
 *       entityId + WARN and never break the business call.
 * </ul>
 */
@Aspect
@Component
public class AuditAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

  private final AuditLogService auditLogService;
  private final AuditorAware<String> auditorAware;

  public AuditAspect(AuditLogService auditLogService, AuditorAware<String> auditorAware) {
    this.auditLogService = auditLogService;
    this.auditorAware = auditorAware;
  }

  @Pointcut("execution(public * com.rainier..*Service.create(..))")
  void createOp() {}

  @Pointcut("execution(public * com.rainier..*Service.update(..))")
  void updateOp() {}

  @Pointcut("execution(public * com.rainier..*Service.delete(..))")
  void deleteOp() {}

  /** create / update — entityId comes from the returned *Detail. */
  @AfterReturning(pointcut = "createOp() || updateOp()", returning = "result")
  public void afterWrite(JoinPoint jp, Object result) {
    String entityType = entityType(jp);
    String action = action(jp);
    Long entityId = idFromResult(result);
    record(entityType, action, entityId);
  }

  /** delete — entityId comes from the first Long argument. */
  @AfterReturning("deleteOp()")
  public void afterDelete(JoinPoint jp) {
    String entityType = entityType(jp);
    Long entityId = idFromArgs(jp);
    record(entityType, AuditAction.DELETE, entityId);
  }

  private void record(String entityType, String action, Long entityId) {
    // v0.0.79 (B6): prefer real loginName from SecurityFilter ThreadLocal (works for callers
    // outside RequestContextHolder too); fall back to AuditorAware (request attribute), then
    // "system" when no identity is bound at all.
    String actor = RequestUserContext.get();
    if (actor == null || actor.isEmpty()) {
      actor = auditorAware.getCurrentAuditor().orElse("system");
    }
    String summary = action + " " + entityType + "#" + entityId;
    auditLogService.record(actor, entityType, entityId, action, summary);
  }

  /** {@code RequirementService} → {@code REQUIREMENT}; {@code SprintFeatureLinkService} →
   * {@code SPRINT_FEATURE_LINK}. Uses the declaring type to avoid CGLIB proxy class names. */
  private String entityType(JoinPoint jp) {
    String simple = ((MethodSignature) jp.getSignature()).getDeclaringType().getSimpleName();
    String base = simple.endsWith("Service") ? simple.substring(0, simple.length() - 7) : simple;
    return base.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
  }

  private String action(JoinPoint jp) {
    return jp.getSignature().getName().toUpperCase();
  }

  private Long idFromResult(Object result) {
    if (result == null) {
      return null;
    }
    try {
      Object id = result.getClass().getMethod("getId").invoke(result);
      return id instanceof Long ? (Long) id : null;
    } catch (ReflectiveOperationException e) {
      log.warn("AuditAspect: could not reflect getId() on {}", result.getClass().getName());
      return null;
    }
  }

  private Long idFromArgs(JoinPoint jp) {
    Object[] args = jp.getArgs();
    return (args.length > 0 && args[0] instanceof Long) ? (Long) args[0] : null;
  }
}
