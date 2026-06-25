/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import com.rainier.auth.service.AuthService;
import com.rainier.authz.PermissionPoint;
import com.rainier.authz.RequiresPermission;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.common.exception.UnauthorizedException;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * v0.0.77 B4 — enforces {@link RequiresPermission @RequiresPermission} on controller methods.
 *
 * <p>Registered AFTER {@code AdminAuthorizationInterceptor} in {@code WebMvcConfig}; only runs once
 * the admin gate (path-prefix + elevation) has already passed. This interceptor adds the SECOND
 * layer — the actual fine-grained permission point check.
 *
 * <p>Gated by {@code app.security.fine-grained-permissions.enabled} (default true). Disabled in the
 * test profile so the ~39 legacy admin controller tests keep passing; the dedicated
 * {@code PermissionInterceptorTest} flips it on.
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";

  private final boolean enabled;
  private final AuthService authService;
  private final PermissionService permissionService;

  public PermissionInterceptor(
      @Value("${app.security.fine-grained-permissions.enabled:true}") boolean enabled,
      AuthService authService,
      PermissionService permissionService) {
    this.enabled = enabled;
    this.authService = authService;
    this.permissionService = permissionService;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!enabled) {
      return true;
    }
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    if (!(handler instanceof HandlerMethod)) {
      return true;
    }
    HandlerMethod hm = (HandlerMethod) handler;
    RequiresPermission ann = hm.getMethodAnnotation(RequiresPermission.class);
    if (ann == null) {
      ann = hm.getBeanType().getAnnotation(RequiresPermission.class);
    }
    if (ann == null || ann.value().length == 0) {
      return true; // unannotated — falls through to AdminPaths tier-A/B defense in depth
    }
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      throw new UnauthorizedException("Missing or malformed Authorization header");
    }
    String username = authService.parseUsername(header.substring(BEARER_PREFIX.length()));
    Set<PermissionPoint> held = permissionService.pointsOfUsername(username);
    PermissionPoint[] required = ann.value();
    boolean ok;
    if (ann.level() == RequiresPermission.Level.ALL) {
      ok = true;
      for (PermissionPoint p : required) {
        if (!held.contains(p)) {
          ok = false;
          break;
        }
      }
    } else {
      ok = false;
      for (PermissionPoint p : required) {
        if (held.contains(p)) {
          ok = true;
          break;
        }
      }
    }
    if (!ok) {
      throw new ForbiddenException("missing required permission");
    }
    return true;
  }
}
