/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Allows browser calls from the frontend dev server (vite) and nginx-served prod build.
 *
 * <p>Covers spec backend-scaffold → "允许前端开发源的 CORS". Origins are configured via {@code
 * app.cors.allowed-origins} in {@code application.yml}.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final List<String> allowedOrigins;

  public CorsConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(allowedOrigins.toArray(new String[0]))
        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin")
        .allowCredentials(false)
        .maxAge(3600);
  }
}
