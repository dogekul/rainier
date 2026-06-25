/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.dto;

/**
 * v0.0.80 B7 — result of a residual-permission recycle operation. {@code revokedCount} is the
 * number of UserRole rows hard-deleted; {@code alreadyDisabled} is meaningful for the combined
 * disable-user endpoint (true when the user was already enabled=false on entry, so only the revoke
 * step ran).
 */
public class RevokeResult {

  private final boolean ok;
  private final int revokedCount;
  private final boolean alreadyDisabled;

  public RevokeResult(boolean ok, int revokedCount, boolean alreadyDisabled) {
    this.ok = ok;
    this.revokedCount = revokedCount;
    this.alreadyDisabled = alreadyDisabled;
  }

  public boolean isOk() {
    return ok;
  }

  public int getRevokedCount() {
    return revokedCount;
  }

  public boolean isAlreadyDisabled() {
    return alreadyDisabled;
  }
}
