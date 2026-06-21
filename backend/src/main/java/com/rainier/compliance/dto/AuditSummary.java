/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.dto;

import com.rainier.auditlog.dto.AuditLogDetail;
import java.util.List;

/** Audit-activity rollup for the compliance dashboard (v0.0.41). */
public class AuditSummary {

  private final long total;
  private final List<LabelCount> byAction;
  private final List<LabelCount> byEntityType;
  private final List<AuditLogDetail> recent;

  public AuditSummary(
      long total,
      List<LabelCount> byAction,
      List<LabelCount> byEntityType,
      List<AuditLogDetail> recent) {
    this.total = total;
    this.byAction = byAction;
    this.byEntityType = byEntityType;
    this.recent = recent;
  }

  public long getTotal() {
    return total;
  }

  public List<LabelCount> getByAction() {
    return byAction;
  }

  public List<LabelCount> getByEntityType() {
    return byEntityType;
  }

  public List<AuditLogDetail> getRecent() {
    return recent;
  }
}
