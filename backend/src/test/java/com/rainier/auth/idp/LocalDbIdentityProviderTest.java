/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.idp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.75 B2 — LocalDb IdentityProvider unit tests (TC-IDP-001 / 002). */
@SpringBootTest
@ActiveProfiles("test")
class LocalDbIdentityProviderTest {

  @Autowired private UserRepository userRepo;
  @Autowired private PasswordEncoder encoder;
  @Autowired private LocalDbIdentityProvider provider;

  @BeforeEach
  void clean() {
    userRepo.deleteAll();
  }

  private User seed(String loginName, String rawPassword, boolean enabled) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName + "-display");
    u.setEmailAddress(loginName + "@rainier.local");
    u.setIsInternal(true);
    u.setEnabled(enabled);
    if (rawPassword != null) {
      u.setPasswordHash(encoder.encode(rawPassword));
    }
    return userRepo.saveAndFlush(u);
  }

  /** TC-IDP-001: correct password → present identity carrying displayName + email. */
  @Test
  void authenticate_correctPassword_returnsIdentity() {
    seed("alice", "s3cret", true);
    Optional<UserIdentity> id = provider.authenticate("alice", "s3cret");
    assertTrue(id.isPresent());
    assertEquals("alice", id.get().getLoginName());
    assertEquals("alice-display", id.get().getDisplayName());
    assertEquals("alice@rainier.local", id.get().getEmail());
    assertEquals("local-db", provider.name());
  }

  /** TC-IDP-002a: wrong password → empty. */
  @Test
  void authenticate_wrongPassword_returnsEmpty() {
    seed("alice", "s3cret", true);
    assertFalse(provider.authenticate("alice", "nope").isPresent());
  }

  /** TC-IDP-002b: unknown user → empty. */
  @Test
  void authenticate_unknownUser_returnsEmpty() {
    assertFalse(provider.authenticate("ghost", "anything").isPresent());
  }

  /** TC-IDP-002c: disabled user → empty (even with correct password). */
  @Test
  void authenticate_disabledUser_returnsEmpty() {
    seed("bob", "s3cret", false);
    assertFalse(provider.authenticate("bob", "s3cret").isPresent());
  }

  /** TC-IDP-002d: null inputs → empty (no NPE). */
  @Test
  void authenticate_nullInputs_returnsEmpty() {
    assertFalse(provider.authenticate(null, "x").isPresent());
    assertFalse(provider.authenticate("x", null).isPresent());
  }
}
