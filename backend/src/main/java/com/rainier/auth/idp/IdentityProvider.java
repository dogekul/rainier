/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.idp;

import java.util.Optional;

/**
 * Pluggable authentication backend. Multiple impls form a chain ordered by Spring {@code @Order};
 * {@code AuthController} tries each one in turn and accepts the first non-empty result.
 *
 * <p>v0.0.75 B2 introduces the SPI; only {@link LocalDbIdentityProvider} is wired by default.
 */
public interface IdentityProvider {

  /** Short stable name for logs / audit (e.g. {@code "local-db"}, {@code "ldap"}). */
  String name();

  /**
   * Verify {@code loginName} + {@code password}. Return empty for any failure (unknown user, wrong
   * password, disabled account, backend not configured, etc.); never throw for "auth failed". Throw
   * only for unexpected infra errors.
   */
  Optional<UserIdentity> authenticate(String loginName, String password);
}
