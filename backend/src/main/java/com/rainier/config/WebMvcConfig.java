/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import com.rainier.authz.AdminAuthorizationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the {@link AdminAuthorizationInterceptor} across all {@code /api/**} requests. The
 * interceptor itself decides per (uri, method) whether the endpoint is gated (see {@code AdminPaths}),
 * so the path list lives in one place rather than being mirrored here.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final AdminAuthorizationInterceptor adminAuthorizationInterceptor;

  public WebMvcConfig(AdminAuthorizationInterceptor adminAuthorizationInterceptor) {
    this.adminAuthorizationInterceptor = adminAuthorizationInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(adminAuthorizationInterceptor).addPathPatterns("/api/**");
  }
}
