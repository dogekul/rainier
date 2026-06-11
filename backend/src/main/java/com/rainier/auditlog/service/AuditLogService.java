/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.service;

import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.dto.AuditLogDetail;
import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read/query operations for {@link AuditLog} plus the {@code record} write used by the audit
 * aspect.
 *
 * <p>NOTE (anti-recursion): method names are deliberately {@code record}/{@code query}/{@code
 * findById} — NEVER {@code create}/{@code update}/{@code delete} — so this service is not matched
 * by {@code AuditAspect}'s pointcuts and writing an audit row does not itself produce an audit row.
 */
@Service
@Transactional(readOnly = true)
public class AuditLogService {

  private final AuditLogRepository repo;

  public AuditLogService(AuditLogRepository repo) {
    this.repo = repo;
  }

  /** Appends an audit row. Called by the aspect within the business transaction. */
  @Transactional
  public AuditLog record(
      String actor, String entityType, Long entityId, String action, String summary) {
    AuditLog log = new AuditLog();
    log.setActor(actor);
    log.setEntityType(entityType);
    log.setEntityId(entityId);
    log.setAction(action);
    log.setSummary(summary);
    return repo.save(log);
  }

  public PageResponse<AuditLogDetail> query(
      String actor, String entityType, Long entityId, String action, PageParams page) {
    Specification<AuditLog> spec =
        (root, q, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (actor != null) {
            p = cb.and(p, cb.equal(root.get("actor"), actor));
          }
          if (entityType != null) {
            p = cb.and(p, cb.equal(root.get("entityType"), entityType));
          }
          if (entityId != null) {
            p = cb.and(p, cb.equal(root.get("entityId"), entityId));
          }
          if (action != null) {
            p = cb.and(p, cb.equal(root.get("action"), action));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
    Page<AuditLog> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(AuditLogDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  public AuditLogDetail findById(Long id) {
    return AuditLogDetail.from(
        repo.findById(id)
            .orElseThrow(() -> new NotFoundException("audit log not found: id=" + id)));
  }
}
