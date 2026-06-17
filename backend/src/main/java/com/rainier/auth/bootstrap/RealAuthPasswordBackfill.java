/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.bootstrap;

import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.38 — when real auth is enabled, give every password-less user (legacy/demo rows, and the column
 * is new) the configured default password (BCrypt) at startup, so existing accounts can still log in.
 * Idempotent (only touches NULL hashes); a no-op when real auth is off (the test profile).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RealAuthPasswordBackfill implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(RealAuthPasswordBackfill.class);

  private final boolean realAuthEnabled;
  private final String defaultPassword;
  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;

  public RealAuthPasswordBackfill(
      @Value("${app.security.real-auth.enabled:false}") boolean realAuthEnabled,
      @Value("${app.security.default-password:rainier123}") String defaultPassword,
      UserRepository userRepo,
      PasswordEncoder passwordEncoder) {
    this.realAuthEnabled = realAuthEnabled;
    this.defaultPassword = defaultPassword;
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!realAuthEnabled) {
      return;
    }
    List<User> changed = new ArrayList<>();
    for (User u : userRepo.findAll()) {
      if (u.getPasswordHash() == null || u.getPasswordHash().isEmpty()) {
        u.setPasswordHash(passwordEncoder.encode(defaultPassword));
        changed.add(u);
      }
    }
    if (!changed.isEmpty()) {
      userRepo.saveAll(changed);
      LOG.info("RealAuthPasswordBackfill: set default password for {} password-less user(s)", changed.size());
    }
  }
}
