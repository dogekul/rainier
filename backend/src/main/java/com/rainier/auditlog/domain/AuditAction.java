/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.domain;

/** Audit action discriminator, stored as VARCHAR(16). Mirrors the audited method names. */
public final class AuditAction {
  public static final String CREATE = "CREATE";
  public static final String UPDATE = "UPDATE";
  public static final String DELETE = "DELETE";

  private AuditAction() {}
}
