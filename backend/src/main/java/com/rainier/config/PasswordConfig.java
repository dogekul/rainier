/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** v0.0.38 — BCrypt password hashing for real credential verification (replaces the mock login). */
@Configuration
public class PasswordConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
