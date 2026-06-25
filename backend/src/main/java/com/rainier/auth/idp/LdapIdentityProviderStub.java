/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.idp;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * LDAP identity provider — STUB only. Wired into the chain when {@code app.auth.ldap.enabled=true};
 * always returns {@code Optional.empty()} until a real LDAP integration ships.
 *
 * <p>v0.0.75 B2 placeholder.
 */
@Component
@ConditionalOnProperty(name = "app.auth.ldap.enabled", havingValue = "true")
@Order(100)
public class LdapIdentityProviderStub implements IdentityProvider {

  @Override
  public String name() {
    return "ldap-stub";
  }

  @Override
  public Optional<UserIdentity> authenticate(String loginName, String password) {
    // Intentional: stub never authenticates anyone.
    return Optional.empty();
  }
}
