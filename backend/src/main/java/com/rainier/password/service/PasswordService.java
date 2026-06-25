/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.password.domain.PasswordResetToken;
import com.rainier.password.repository.PasswordResetTokenRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.76 B3 — password management: self-change, admin reset, forgot/reset flow.
 *
 * <p>All persistence touches go through {@link UserRepository#saveAndFlush(Object)} so a follow-up
 * login in the same test sees the new hash.
 */
@Service
public class PasswordService {

  private static final Logger LOG = LoggerFactory.getLogger(PasswordService.class);

  /** Minimum length enforced on every newPassword input. */
  static final int MIN_LENGTH = 8;

  /** Token TTL from issuance. */
  static final long TOKEN_TTL_MINUTES = 60L;

  private final UserRepository userRepo;
  private final PasswordResetTokenRepository tokenRepo;
  private final PasswordEncoder encoder;

  public PasswordService(
      UserRepository userRepo,
      PasswordResetTokenRepository tokenRepo,
      PasswordEncoder encoder) {
    this.userRepo = userRepo;
    this.tokenRepo = tokenRepo;
    this.encoder = encoder;
  }

  /** {@code POST /api/me/password} — verify currentPassword, then overwrite hash. */
  @Transactional
  public void changeOwnPassword(String loginName, String currentPassword, String newPassword) {
    if (isBlank(currentPassword)) {
      throw new BadRequestException("currentPassword is required");
    }
    requireValidNewPassword(newPassword);
    User user =
        userRepo
            .findByLoginName(loginName)
            .orElseThrow(() -> new UnauthorizedException("invalid credentials"));
    if (user.getPasswordHash() == null
        || !encoder.matches(currentPassword, user.getPasswordHash())) {
      // Keep 401 generic — never tell the caller whether the user exists or the password was wrong.
      throw new UnauthorizedException("invalid credentials");
    }
    user.setPasswordHash(encoder.encode(newPassword));
    userRepo.saveAndFlush(user);
  }

  /** {@code POST /api/admin/users/{id}/reset-password} — admin override, no current-pass check. */
  @Transactional
  public void adminResetPassword(Long userId, String newPassword) {
    requireValidNewPassword(newPassword);
    User user =
        userRepo
            .findById(userId)
            .orElseThrow(() -> new NotFoundException("user not found: " + userId));
    user.setPasswordHash(encoder.encode(newPassword));
    userRepo.saveAndFlush(user);
  }

  /**
   * {@code POST /api/auth/forgot-password} — issue a 1h token when (loginName, email) matches.
   * Silent no-op otherwise (anti-enumeration). Always returns normally.
   */
  @Transactional
  public void issueResetToken(String loginName, String email) {
    if (isBlank(loginName) || isBlank(email)) {
      throw new BadRequestException("loginName and email are required");
    }
    Optional<User> maybe = userRepo.findByLoginName(loginName.trim());
    if (!maybe.isPresent()) {
      LOG.info("[password-reset] forgot-password ignored — unknown loginName='{}'", loginName);
      return;
    }
    User user = maybe.get();
    String storedEmail = user.getEmailAddress();
    if (storedEmail == null || !storedEmail.trim().equalsIgnoreCase(email.trim())) {
      LOG.info("[password-reset] forgot-password ignored — email mismatch loginName='{}'", loginName);
      return;
    }
    PasswordResetToken token = new PasswordResetToken();
    token.setUserId(user.getId());
    token.setToken(UUID.randomUUID().toString().replace("-", ""));
    Instant now = Instant.now();
    token.setExpiresAt(now.plus(TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
    tokenRepo.saveAndFlush(token);
    // DEV ONLY — in prod this is replaced by an email send.
    LOG.info(
        "[password-reset] issued token={} userId={} loginName={} expiresAt={}",
        token.getToken(),
        user.getId(),
        loginName,
        token.getExpiresAt());
  }

  /** {@code POST /api/auth/reset-password} — consume token + overwrite hash. */
  @Transactional
  public void redeemResetToken(String tokenValue, String newPassword) {
    if (isBlank(tokenValue)) {
      throw new BadRequestException("token is required");
    }
    requireValidNewPassword(newPassword);
    PasswordResetToken token =
        tokenRepo
            .findByToken(tokenValue)
            .orElseThrow(() -> new BadRequestException("invalid or expired token"));
    Instant now = Instant.now();
    if (token.getUsedAt() != null) {
      throw new BadRequestException("invalid or expired token");
    }
    if (token.getExpiresAt() == null || !token.getExpiresAt().isAfter(now)) {
      throw new BadRequestException("invalid or expired token");
    }
    User user =
        userRepo
            .findById(token.getUserId())
            .orElseThrow(() -> new BadRequestException("invalid or expired token"));
    user.setPasswordHash(encoder.encode(newPassword));
    userRepo.saveAndFlush(user);
    token.setUsedAt(now);
    tokenRepo.saveAndFlush(token);
  }

  private static void requireValidNewPassword(String newPassword) {
    if (newPassword == null || newPassword.length() < MIN_LENGTH) {
      throw new BadRequestException("newPassword must be at least " + MIN_LENGTH + " characters");
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
