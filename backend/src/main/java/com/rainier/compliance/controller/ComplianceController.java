/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.controller;

import com.rainier.authz.PermissionPoint;
import com.rainier.authz.RequiresPermission;
import com.rainier.compliance.dto.AuditSummary;
import com.rainier.compliance.dto.ResidualPermission;
import com.rainier.compliance.dto.RevokeResult;
import com.rainier.compliance.service.ComplianceService;
import com.rainier.compliance.service.ResidualPermissionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin compliance dashboard (v0.0.41 + v0.0.80 B7 write actions). {@code /api/compliance/**} is
 * admin-gated via {@code AdminPaths} Tier A (the {@code AdminAuthorizationInterceptor} enforces
 * token + elevation). Reads aggregate audit activity + the「停用-残留权限」reconciliation; writes
 * (v0.0.80) one-click revoke the residual role grants and optionally disable+revoke in one step.
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

  private final ComplianceService service;
  private final ResidualPermissionService residualService;

  public ComplianceController(ComplianceService service, ResidualPermissionService residualService) {
    this.service = service;
    this.residualService = residualService;
  }

  @GetMapping(path = "/audit-summary", produces = "application/json")
  @RequiresPermission({PermissionPoint.COMPLIANCE_VIEW, PermissionPoint.AUDIT_VIEW})
  public AuditSummary auditSummary() {
    return service.auditSummary();
  }

  @GetMapping(path = "/residual-permissions", produces = "application/json")
  @RequiresPermission(PermissionPoint.COMPLIANCE_VIEW)
  public List<ResidualPermission> residualPermissions() {
    return service.residualPermissions();
  }

  /** v0.0.80 B7 — hard-delete all UserRoles of a disabled user. 400 if user is still enabled. */
  @PostMapping(path = "/users/{id}/revoke-roles", produces = "application/json")
  @RequiresPermission(PermissionPoint.COMPLIANCE_VIEW)
  public RevokeResult revokeRoles(@PathVariable("id") Long userId) {
    return residualService.revokeAllRoles(userId);
  }

  /** v0.0.80 B7 — disable user + revoke all UserRoles in one step (idempotent on enabled flag). */
  @PostMapping(path = "/disable-user/{id}", produces = "application/json")
  @RequiresPermission(PermissionPoint.COMPLIANCE_VIEW)
  public RevokeResult disableUser(@PathVariable("id") Long userId) {
    return residualService.disableAndRevoke(userId);
  }
}
