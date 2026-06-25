/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import org.springframework.http.HttpMethod;

/**
 * Central list of {@code /api/**} endpoints that are exempt from the {@code
 * app.security.require-all-users-token} baseline gate (v0.0.27, formalised v0.0.74-B1).
 *
 * <p>Kept as a separate class so production code (SecurityFilter) and tests both reference the same
 * source of truth without hard-coding strings. CORS preflight (OPTIONS) is also unconditionally
 * exempt — handled inside {@link #isWhitelisted(String, String)}.
 *
 * <p>Whitelisted endpoints:
 *
 * <ul>
 *   <li>{@code POST /api/auth/login} — credential exchange; obviously cannot require a token
 *   <li>{@code GET /api/health} — liveness probe; must answer to anonymous monitors
 *   <li>Any {@code OPTIONS /api/**} — CORS preflight carries no token by design
 * </ul>
 */
public final class SecurityWhitelistPaths {

  public static final String LOGIN_PATH = "/api/auth/login";
  public static final String HEALTH_PATH = "/api/health";
  /** v0.0.76 B3 — anonymous password recovery: issue token by (loginName, email). */
  public static final String FORGOT_PASSWORD_PATH = "/api/auth/forgot-password";
  /** v0.0.76 B3 — anonymous password recovery: redeem token + set new password. */
  public static final String RESET_PASSWORD_PATH = "/api/auth/reset-password";

  private SecurityWhitelistPaths() {}

  /**
   * Returns true when the given (method, path) pair is exempt from the baseline token gate. {@code
   * path} MUST be the semicolon-stripped request URI (see {@code UrlPathHelper.getRequestUri}) so
   * matrix-parameter variants such as {@code /api/health;x=1} do not bypass.
   */
  public static boolean isWhitelisted(String method, String path) {
    if (HttpMethod.OPTIONS.matches(method)) {
      return true;
    }
    if (path == null) {
      return false;
    }
    if (HttpMethod.POST.matches(method) && LOGIN_PATH.equals(path)) {
      return true;
    }
    if (HttpMethod.POST.matches(method) && FORGOT_PASSWORD_PATH.equals(path)) {
      return true;
    }
    if (HttpMethod.POST.matches(method) && RESET_PASSWORD_PATH.equals(path)) {
      return true;
    }
    if (HttpMethod.GET.matches(method) && HEALTH_PATH.equals(path)) {
      return true;
    }
    return false;
  }
}
