/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import com.rainier.authz.AdminAuthorizationInterceptor;
import com.rainier.authz.permission.PermissionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link AdminAuthorizationInterceptor} across all {@code /api/**} requests. The
 * interceptor itself decides per (uri, method) whether the endpoint is gated (see {@code AdminPaths}),
 * so the path list lives in one place rather than being mirrored here.
 *
 * <p>v0.0.77 B4 adds {@link PermissionInterceptor} AFTER the admin gate to enforce method-level
 * {@code @RequiresPermission} fine-grained points (second defense layer on top of the path-prefix +
 * elevation gate).
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AdminAuthorizationInterceptor adminAuthorizationInterceptor;
  private final PermissionInterceptor permissionInterceptor;

  public WebMvcConfig(
      AdminAuthorizationInterceptor adminAuthorizationInterceptor,
      PermissionInterceptor permissionInterceptor) {
    this.adminAuthorizationInterceptor = adminAuthorizationInterceptor;
    this.permissionInterceptor = permissionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminAuthorizationInterceptor).addPathPatterns("/api/**");
    registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**");
  }
}
