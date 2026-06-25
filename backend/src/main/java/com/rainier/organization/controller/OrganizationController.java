/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import com.rainier.auditlog.dto.AuditLogDetail;
import com.rainier.auditlog.service.AuditLogService;
import com.rainier.authz.PermissionPoint;
import com.rainier.authz.RequiresPermission;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.dto.OrganizationCreateRequest;
import com.rainier.organization.dto.OrganizationDetail;
import com.rainier.organization.dto.OrganizationMoveRequest;
import com.rainier.organization.dto.OrganizationUpdateRequest;
import com.rainier.organization.service.OrganizationService;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for the organization tree. */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

  /** AuditLog entityType used by AuditAspect for OrganizationService writes. */
  private static final String ORG_AUDIT_TYPE = "ORGANIZATION";

  private final OrganizationService service;
  private final AuditLogService auditLogService;

  public OrganizationController(OrganizationService service, AuditLogService auditLogService) {
    this.service = service;
    this.auditLogService = auditLogService;
  }

  @PostMapping
  public ResponseEntity<OrganizationDetail> create(
      @Valid @RequestBody OrganizationCreateRequest req) {
    OrganizationDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/organizations/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public OrganizationDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping("/tree")
  public List<OrganizationDetail> tree() {
    return service.findTree();
  }

  @GetMapping
  public PageResponse<OrganizationDetail> list(
      @RequestParam(required = false) OrganizationType type,
      @RequestParam(name = "parentId", required = false) Long parentId,
      @Valid PageParams page) {
    return service.list(type, parentId, page);
  }

  @PutMapping("/{id}")
  public OrganizationDetail update(
      @PathVariable Long id, @Valid @RequestBody OrganizationUpdateRequest req) {
    return service.update(id, req);
  }

  @PutMapping("/{id}/parent")
  public OrganizationDetail move(
      @PathVariable Long id, @Valid @RequestBody OrganizationMoveRequest req) {
    return service.move(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * v0.0.99 (E3) — 该组织节点维度的审计历史。
   *
   * <p>委托给 {@link AuditLogService#query} 过滤 entityType=ORGANIZATION + entityId=id。先调用
   * {@link OrganizationService#findById} 触发 NotFound（组织不存在时 → 404，与其它接口一致）。
   *
   * <p>本版只覆盖 ORGANIZATION 本身的写入；ORGANIZATION_PMO 行不在聚合范围内（spec OutOfScope）。
   */
  @GetMapping("/{id}/audit-log")
  @RequiresPermission(PermissionPoint.AUDIT_VIEW)
  public PageResponse<AuditLogDetail> auditLog(
      @PathVariable Long id,
      @RequestParam(required = false) String action,
      @Valid PageParams page) {
    service.findById(id);
    return auditLogService.query(null, ORG_AUDIT_TYPE, id, action, page);
  }
}
