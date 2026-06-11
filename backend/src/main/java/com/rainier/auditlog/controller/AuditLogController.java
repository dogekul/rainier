/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.controller;

import com.rainier.auditlog.dto.AuditLogDetail;
import com.rainier.auditlog.service.AuditLogService;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only endpoints for {@link com.rainier.auditlog.domain.AuditLog}. Append-only — there is
 * intentionally no write mapping (no POST / PUT / DELETE handler).
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

  private final AuditLogService service;

  public AuditLogController(AuditLogService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<AuditLogDetail> list(
      @RequestParam(required = false) String actor,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) Long entityId,
      @RequestParam(required = false) String action,
      @Valid PageParams page) {
    return service.query(actor, entityType, entityId, action, page);
  }

  @GetMapping("/{id}")
  public AuditLogDetail get(@PathVariable Long id) {
    return service.findById(id);
  }
}
