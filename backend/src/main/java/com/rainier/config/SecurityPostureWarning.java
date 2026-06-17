/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * v0.0.38 — fails OPEN loudly, not silently. A security-critical control that's misconfigured should
 * scream in the log. On startup (outside the test profile) this WARNs when the auth posture is the
 * insecure dev default, so an operator sees it before going to production. It does NOT fail-fast — the
 * dev/demo default password (rainier123) is an intentional choice (see proposal), so blocking boot would
 * break the demo; the loud warning is the signal. Prod hardening (override the secret + default password,
 * add a change-password flow) is the follow-up.
 */
@Component
public class SecurityPostureWarning implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SecurityPostureWarning.class);

  private final Environment env;
  private final boolean realAuthEnabled;
  private final String defaultPassword;
  private final String jwtSecret;

  public SecurityPostureWarning(
      Environment env,
      @Value("${app.security.real-auth.enabled:false}") boolean realAuthEnabled,
      @Value("${app.security.default-password:rainier123}") String defaultPassword,
      @Value("${app.security.jwt.secret:}") String jwtSecret) {
    this.env = env;
    this.realAuthEnabled = realAuthEnabled;
    this.defaultPassword = defaultPassword;
    this.jwtSecret = jwtSecret;
  }

  @Override
  public void run(String... args) {
    for (String p : env.getActiveProfiles()) {
      if ("test".equals(p)) {
        return; // tests intentionally run with the mock/dev posture
      }
    }
    if (!realAuthEnabled) {
      LOG.warn(
          "SECURITY: real-auth is DISABLED — login accepts ANY password for any username. "
              + "Set app.security.real-auth.enabled=true before production.");
    }
    if (realAuthEnabled && "rainier123".equals(defaultPassword)) {
      LOG.warn(
          "SECURITY: using the INSECURE shared default password 'rainier123' for password-less users. "
              + "Override app.security.default-password and add a change-password flow before production.");
    }
    if (jwtSecret.contains("changeme") || jwtSecret.contains("dev-only")) {
      LOG.warn(
          "SECURITY: using the dev JWT signing secret — tokens are FORGEABLE by anyone with the repo. "
              + "Override app.security.jwt.secret (256-bit) before production.");
    }
  }
}
