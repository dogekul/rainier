/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.controller;

import com.rainier.authz.PermissionPoint;
import com.rainier.authz.RequiresPermission;
import com.rainier.compliance.dto.AuditSummary;
import com.rainier.compliance.dto.ResidualPermission;
import com.rainier.compliance.service.ComplianceService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin compliance dashboard (v0.0.41). {@code /api/compliance/**} is admin-gated via {@code
 * AdminPaths} Tier A (the {@code AdminAuthorizationInterceptor} enforces token + elevation). Read-only
 * aggregation — audit activity rollup + the「停用-残留权限」reconciliation.
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

  private final ComplianceService service;

  public ComplianceController(ComplianceService service) {
    this.service = service;
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
}
