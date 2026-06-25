/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.idp;

import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Collections;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Default identity provider — verifies BCrypt hashes stored in the local {@code rainier_user}
 * table. Highest precedence in the provider chain.
 *
 * <p>v0.0.75 B2: extracted from {@code AuthController.login} so that LDAP/OAuth/SAML can plug in
 * later via the same SPI.
 */
@Component
@Primary
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LocalDbIdentityProvider implements IdentityProvider {

  private final UserRepository userRepo;
  private final PasswordEncoder passwordEncoder;

  public LocalDbIdentityProvider(UserRepository userRepo, PasswordEncoder passwordEncoder) {
    this.userRepo = userRepo;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public String name() {
    return "local-db";
  }

  @Override
  public Optional<UserIdentity> authenticate(String loginName, String password) {
    if (loginName == null || password == null) {
      return Optional.empty();
    }
    User user = userRepo.findByLoginName(loginName).orElse(null);
    if (user == null
        || !Boolean.TRUE.equals(user.getEnabled())
        || user.getPasswordHash() == null
        || !passwordEncoder.matches(password, user.getPasswordHash())) {
      return Optional.empty();
    }
    String externalId = user.getId() == null ? loginName : String.valueOf(user.getId());
    return Optional.of(
        new UserIdentity(
            externalId,
            user.getLoginName(),
            user.getName(),
            user.getEmailAddress(),
            Collections.<String>emptyList()));
  }
}
