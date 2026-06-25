/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * One-time password-reset token issued by {@code POST /api/auth/forgot-password} and consumed by
 * {@code POST /api/auth/reset-password}.
 *
 * <p>v0.0.76 B3 — short-lived (default 1h TTL). NOT soft-deleted; the {@code used_at} column
 * encodes consumption so the service can reject re-use without losing the audit trail.
 */
@Entity
@Table(name = "rainier_password_reset_token")
public class PasswordResetToken extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "token", nullable = false, length = 64)
  private String token;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  /** NULL while the token is still redeemable; set to {@code now()} on consumption. */
  @Column(name = "used_at")
  private Instant usedAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(Instant usedAt) {
    this.usedAt = usedAt;
  }
}
