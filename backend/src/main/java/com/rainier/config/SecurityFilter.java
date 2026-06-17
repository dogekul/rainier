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
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Verifies {@code Authorization: Bearer <token>} on {@code /api/auth/me} only and exposes the
 * parsed username as a request attribute. Other endpoints are unaffected.
 *
 * <p>Token validation failures are intentionally swallowed here so that the controller can throw
 * {@link UnauthorizedException}, which is uniformly translated to a JSON 401 by {@code
 * GlobalExceptionHandler}.
 *
 * <p>Covers spec auth-placeholder → "凭 token 查询当前用户".
 */
@Component
public class SecurityFilter extends OncePerRequestFilter {

  private static final String ME_PATH = "/api/auth/me";
  private static final String SELF_SCOPE_PREFIX = "/api/me/";
  private static final String BEARER_PREFIX = "Bearer ";

  /** Paths that resolve the Bearer token into the {@code rainier.username} request attribute. */
  private static boolean needsIdentity(String uri) {
    return ME_PATH.equals(uri) || (uri != null && uri.startsWith(SELF_SCOPE_PREFIX));
  }

  private final AuthService authService;

  public SecurityFilter(AuthService authService) {
    this.authService = authService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {

    if (needsIdentity(request.getRequestURI())) {
      String header = request.getHeader("Authorization");
      if (header != null && header.startsWith(BEARER_PREFIX)) {
        String token = header.substring(BEARER_PREFIX.length());
        try {
          String username = authService.parseUsername(token);
          request.setAttribute(AuthController.ATTR_USERNAME, username);
        } catch (UnauthorizedException ex) {
          // Leave attribute unset; controller will throw and GlobalExceptionHandler will produce
          // a JSON 401. We do NOT propagate from the filter to keep error formatting uniform.
        }
      }
    }
    chain.doFilter(request, response);
  }
}
