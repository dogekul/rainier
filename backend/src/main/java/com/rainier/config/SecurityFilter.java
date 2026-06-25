/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import com.rainier.auth.controller.AuthController;
import com.rainier.auth.service.AuthService;
import com.rainier.common.exception.UnauthorizedException;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/**
 * Resolves {@code Authorization: Bearer <token>} into the {@code rainier.username} request attribute
 * (identity), and — when {@code app.security.require-all-users-token.enabled=true} (v0.0.27) — enforces
 * that every {@code /api/**} request carries a valid token, returning a uniform JSON 401 otherwise.
 *
 * <p>Identity is resolved for ANY path so it flows to every downstream endpoint. The baseline gate is
 * flag-gated (default false, false in the test profile) so the legacy unauthenticated tests stay green;
 * a dedicated {@code AuthBaselineTest} flips it on. This filter runs BEFORE {@code
 * AdminAuthorizationInterceptor}, so a missing token yields 401 (identity) before any 403 (authz).
 *
 * <p>Whitelist: {@code POST /api/auth/login} and {@code GET /api/health} (and CORS preflight) never
 * require a token. Matrix-parameter bypass is avoided by matching on the {@link UrlPathHelper}
 * semicolon-stripped request URI (same defense as v0.0.21).
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

  private static final String API_PREFIX = "/api/";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final UrlPathHelper PATH_HELPER = new UrlPathHelper();

  private final AuthService authService;
  private final boolean requireAllUsersToken;

  public SecurityFilter(
      AuthService authService,
      @Value("${app.security.require-all-users-token.enabled:false}") boolean requireAllUsersToken) {
    this.authService = authService;
    this.requireAllUsersToken = requireAllUsersToken;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    // Resolve identity from a valid Bearer token (any path), so it flows to every endpoint.
    String username = resolveUsername(request);
    if (username != null) {
      request.setAttribute(AuthController.ATTR_USERNAME, username);
    }

    // Baseline gate: every /api/** (minus the whitelist) requires a valid token when enabled.
    if (requireAllUsersToken && username == null) {
      String path = PATH_HELPER.getRequestUri(request); // semicolon-stripped → no matrix bypass
      if (isGuarded(path, request.getMethod())) {
        writeUnauthorized(response);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  /** Returns the token subject for a valid Bearer token, or null when absent/invalid. */
  private String resolveUsername(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith(BEARER_PREFIX)) {
      try {
        return authService.parseUsername(header.substring(BEARER_PREFIX.length()));
      } catch (UnauthorizedException ex) {
        return null; // invalid/expired token → treated as no identity
      }
    }
    return null;
  }

  private static boolean isGuarded(String path, String method) {
    if (path == null || !path.startsWith(API_PREFIX)) {
      return false; // non-API (static assets etc.) untouched
    }
    return !SecurityWhitelistPaths.isWhitelisted(method, path);
  }

  private static void writeUnauthorized(HttpServletResponse response) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"message\":\"Missing or invalid token\"}");
  }
}
