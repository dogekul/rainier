/* (C) 2026 Rainier — internal use only. */
package com.rainier.password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.password.domain.PasswordResetToken;
import com.rainier.password.repository.PasswordResetTokenRepository;
import com.rainier.password.service.PasswordService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.76 B3 — covers TC-PWD-001..010. Mixes service-layer unit assertions with MockMvc to verify
 * the HTTP surface (whitelist exemption, JSON contract).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordServiceTest {

  @Autowired private PasswordService service;
  @Autowired private UserRepository userRepo;
  @Autowired private PasswordResetTokenRepository tokenRepo;
  @Autowired private PasswordEncoder encoder;
  @Autowired private AuthService authService;
  @Autowired private MockMvc mockMvc;

  private Long aliceId;

  @BeforeEach
  void seed() {
    tokenRepo.deleteAll();
    userRepo.deleteAll();
    User alice = new User();
    alice.setLoginName("alice");
    alice.setName("Alice");
    alice.setEmailAddress("alice@example.com");
    alice.setIsInternal(true);
    alice.setEnabled(true);
    alice.setPasswordHash(encoder.encode("s3cretPW"));
    aliceId = userRepo.saveAndFlush(alice).getId();
  }

  /** TC-PWD-001: change own password — correct current → success. */
  @Test
  void changeOwn_correctCurrent_updatesHash() {
    service.changeOwnPassword("alice", "s3cretPW", "newPass123");
    User reloaded = userRepo.findByLoginName("alice").get();
    assertTrue(encoder.matches("newPass123", reloaded.getPasswordHash()));
    assertFalse(encoder.matches("s3cretPW", reloaded.getPasswordHash()));
  }

  /** TC-PWD-002: change own password — wrong current → 401. */
  @Test
  void changeOwn_wrongCurrent_throws401() {
    assertThrows(
        UnauthorizedException.class,
        () -> service.changeOwnPassword("alice", "wrongPW1", "newPass123"));
    User reloaded = userRepo.findByLoginName("alice").get();
    assertTrue(encoder.matches("s3cretPW", reloaded.getPasswordHash()));
  }

  /** TC-PWD-003: change own password — newPassword too short → 400. */
  @Test
  void changeOwn_shortNewPassword_throws400() {
    assertThrows(
        BadRequestException.class,
        () -> service.changeOwnPassword("alice", "s3cretPW", "short1"));
  }

  /** TC-PWD-004: admin reset → success. */
  @Test
  void adminReset_overwritesHash() {
    service.adminResetPassword(aliceId, "adminSet1");
    User reloaded = userRepo.findById(aliceId).get();
    assertTrue(encoder.matches("adminSet1", reloaded.getPasswordHash()));
  }

  /** TC-PWD-005: admin reset — user not found → 404. */
  @Test
  void adminReset_userNotFound_throws404() {
    assertThrows(
        NotFoundException.class, () -> service.adminResetPassword(999_999L, "adminSet1"));
  }

  /** TC-PWD-006: forgot + reset happy path. */
  @Test
  void forgotThenReset_happyPath() {
    service.issueResetToken("alice", "alice@example.com");
    List<PasswordResetToken> tokens = tokenRepo.findAll();
    assertEquals(1, tokens.size());
    PasswordResetToken issued = tokens.get(0);
    assertNotNull(issued.getToken());
    assertNull(issued.getUsedAt());
    assertEquals(aliceId, issued.getUserId());

    service.redeemResetToken(issued.getToken(), "freshPW01");
    User reloaded = userRepo.findById(aliceId).get();
    assertTrue(encoder.matches("freshPW01", reloaded.getPasswordHash()));
    PasswordResetToken consumed = tokenRepo.findByToken(issued.getToken()).get();
    assertNotNull(consumed.getUsedAt());
  }

  /** TC-PWD-007: forgot — wrong email → silent 200, no token row. */
  @Test
  void forgot_emailMismatch_isSilentNoOp() {
    service.issueResetToken("alice", "imposter@example.com");
    assertTrue(tokenRepo.findAll().isEmpty());
  }

  /** TC-PWD-007b: forgot — unknown loginName → silent 200, no token row. */
  @Test
  void forgot_unknownLoginName_isSilentNoOp() {
    service.issueResetToken("ghost", "ghost@example.com");
    assertTrue(tokenRepo.findAll().isEmpty());
  }

  /** TC-PWD-008: reset — bogus token → 400. */
  @Test
  void reset_bogusToken_throws400() {
    assertThrows(
        BadRequestException.class, () -> service.redeemResetToken("not-a-real-token", "freshPW01"));
  }

  /** TC-PWD-009: reset — expired token → 400. */
  @Test
  void reset_expiredToken_throws400() {
    PasswordResetToken expired = new PasswordResetToken();
    expired.setUserId(aliceId);
    expired.setToken("expired-abc");
    expired.setExpiresAt(Instant.now().minus(5, ChronoUnit.MINUTES));
    tokenRepo.saveAndFlush(expired);
    assertThrows(
        BadRequestException.class, () -> service.redeemResetToken("expired-abc", "freshPW01"));
  }

  /** TC-PWD-010: reset — token re-use → 400. */
  @Test
  void reset_usedToken_throws400() {
    service.issueResetToken("alice", "alice@example.com");
    PasswordResetToken token = tokenRepo.findAll().get(0);
    service.redeemResetToken(token.getToken(), "firstUse1");
    assertThrows(
        BadRequestException.class,
        () -> service.redeemResetToken(token.getToken(), "secondUse2"));
  }

  // ----- HTTP surface (whitelist + admin route) -----

  /** Whitelist: POST /api/auth/forgot-password does NOT require a token. */
  @Test
  void httpForgotPassword_anonymous_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"loginName\":\"alice\",\"email\":\"alice@example.com\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
    assertEquals(1, tokenRepo.findAll().size());
  }

  /** Whitelist: POST /api/auth/reset-password does NOT require a token. */
  @Test
  void httpResetPassword_anonymous_returns200() throws Exception {
    service.issueResetToken("alice", "alice@example.com");
    String tk = tokenRepo.findAll().get(0).getToken();
    mockMvc
        .perform(
            post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + tk + "\",\"newPassword\":\"httpPath1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
  }

  /** Authenticated: POST /api/me/password with valid bearer token. */
  @Test
  void httpChangeOwnPassword_withToken_returns200() throws Exception {
    String token = authService.issueToken("alice");
    mockMvc
        .perform(
            post("/api/me/password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"s3cretPW\",\"newPassword\":\"httpChg01\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
    User reloaded = userRepo.findByLoginName("alice").get();
    assertTrue(encoder.matches("httpChg01", reloaded.getPasswordHash()));
  }

  /** Admin route — admin-authz is OFF in default test profile, so it just executes. */
  @Test
  void httpAdminReset_returns200() throws Exception {
    mockMvc
        .perform(
            post("/api/admin/users/" + aliceId + "/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newPassword\":\"adminSet1\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
    User reloaded = userRepo.findById(aliceId).get();
    assertTrue(encoder.matches("adminSet1", reloaded.getPasswordHash()));
  }
}
