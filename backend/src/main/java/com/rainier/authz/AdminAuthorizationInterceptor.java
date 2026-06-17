/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import com.rainier.auth.service.AuthService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.common.exception.UnauthorizedException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UrlPathHelper;

/**
 * Authorizes admin-only endpoints (v0.0.21). For a gated (uri, method) — see {@link AdminPaths} — a
 * missing/invalid bearer token yields 401 ({@link UnauthorizedException}) and an authenticated but
 * non-elevated caller yields 403 ({@link ForbiddenException}); both are rendered as JSON by {@code
 * GlobalExceptionHandler}. All-users endpoints, Tier-B GETs and CORS preflight pass through.
 *
 * <p>Gated by {@code app.security.admin-authz.enabled} — false in the test profile so the ~39 legacy
 * admin controller tests run unauthenticated unchanged; the dedicated {@code AdminAuthorizationTest}
 * flips it on. This is frontend-scope-completing security hardening of the v0.0.20 role split.
 */
@Component
public class AdminAuthorizationInterceptor implements HandlerInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";

  /**
   * Match on the SAME lookup path the DispatcherServlet routes on — not the raw {@code
   * getRequestURI()}. Defaults remove matrix-parameter (;…) segments and URL-decode, so a caller
   * can't slip past the gate with {@code /api/roles;x=1} (which Tomcat strips before routing to the
   * controller, but the raw URI would not match here). Closes the matrix-param authz bypass.
   */
  private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

  private final boolean enabled;
  private final AuthService authService;
  private final ElevationService elevationService;

  public AdminAuthorizationInterceptor(
      @Value("${app.security.admin-authz.enabled:true}") boolean enabled,
      AuthService authService,
      ElevationService elevationService) {
    this.enabled = enabled;
    this.authService = authService;
    this.elevationService = elevationService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!enabled) {
      return true;
    }
    String method = request.getMethod();
    if ("OPTIONS".equalsIgnoreCase(method)) {
      return true; // never gate CORS preflight
    }
    String path = URL_PATH_HELPER.getLookupPathForRequest(request);
    if (!AdminPaths.requiresAdmin(path, method)) {
      return true; // all-users endpoint or a Tier-B read
    }
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      throw new UnauthorizedException("Missing or malformed Authorization header");
    }
    String username = authService.parseUsername(header.substring(BEARER_PREFIX.length()));
    if (!elevationService.isElevated(username)) {
      throw new ForbiddenException("admin access required");
    }
    return true;
  }
}
