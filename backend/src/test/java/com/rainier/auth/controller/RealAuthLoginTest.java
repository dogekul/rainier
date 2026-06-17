/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.bootstrap.RealAuthPasswordBackfill;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.38 — real credential verification (flag on). Covers TC-AUTH-REAL-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {"app.security.real-auth.enabled=true", "app.security.default-password=rainier123"})
class RealAuthLoginTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepo;
  @Autowired private PasswordEncoder encoder;
  @Autowired private RealAuthPasswordBackfill backfill;

  @BeforeEach
  void cleanDb() {
    userRepo.deleteAll();
  }

  private User seedUser(String loginName, String rawPassword, boolean enabled) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(enabled);
    if (rawPassword != null) {
      u.setPasswordHash(encoder.encode(rawPassword));
    }
    return userRepo.saveAndFlush(u);
  }

  private String body(String username, String password) {
    return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
  }

  /** TC-AUTH-REAL-001: correct password → 200 + token. */
  @Test
  void login_correctPassword_returns200() throws Exception {
    seedUser("alice", "s3cret", true);
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("alice", "s3cret")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.user.username").value("alice"));
  }

  /** TC-AUTH-REAL-002: wrong password → 401. */
  @Test
  void login_wrongPassword_returns401() throws Exception {
    seedUser("alice", "s3cret", true);
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("alice", "nope")))
        .andExpect(status().isUnauthorized());
  }

  /** TC-AUTH-REAL-003: unknown user → 401 (same message as wrong password). */
  @Test
  void login_unknownUser_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("ghost", "whatever")))
        .andExpect(status().isUnauthorized());
  }

  /** TC-AUTH-REAL-004: disabled user → 401 even with the right password. */
  @Test
  void login_disabledUser_returns401() throws Exception {
    seedUser("bob", "s3cret", false);
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("bob", "s3cret")))
        .andExpect(status().isUnauthorized());
  }

  /** TC-AUTH-REAL-005: missing password → 400 (validation before verification). */
  @Test
  void login_missingPassword_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"alice\"}"))
        .andExpect(status().isBadRequest());
  }

  /** TC-AUTH-REAL-006: backfill sets the default password for a password-less user; then they can log in. */
  @Test
  void backfill_setsDefaultPassword_thenLoginWorks() throws Exception {
    seedUser("legacy", null, true); // no password hash
    backfill.run();

    User reloaded =
        userRepo.findByLoginName("legacy").orElseThrow(() -> new AssertionError("legacy user missing"));
    assertNotNull(reloaded.getPasswordHash());
    assertTrue(encoder.matches("rainier123", reloaded.getPasswordHash()));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("legacy", "rainier123")))
        .andExpect(status().isOk());
  }
}
