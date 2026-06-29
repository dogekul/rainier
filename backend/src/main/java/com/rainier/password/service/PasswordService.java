/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.email.EmailMessage;
import com.rainier.email.EmailSender;
import com.rainier.email.SendResult;
import com.rainier.password.domain.PasswordResetToken;
import com.rainier.password.repository.PasswordResetTokenRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
  private final EmailSender emailSender;
  private final String frontendBaseUrl;

  public PasswordService(
      UserRepository userRepo,
      PasswordResetTokenRepository tokenRepo,
      PasswordEncoder encoder,
      EmailSender emailSender,
      @Value("${app.frontend.base-url:http://localhost}") String frontendBaseUrl) {
    this.userRepo = userRepo;
    this.tokenRepo = tokenRepo;
    this.encoder = encoder;
    this.emailSender = emailSender;
    this.frontendBaseUrl = stripTrailingSlash(frontendBaseUrl);
  }

  private static String stripTrailingSlash(String s) {
    if (s == null) {
      return "http://localhost";
    }
    String trimmed = s.trim();
    if (trimmed.endsWith("/")) {
      return trimmed.substring(0, trimmed.length() - 1);
    }
    return trimmed;
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
    // INFO log token as dev fallback — kept on purpose so a broken SMTP relay does not lock users out
    // (operator can still recover the token from logs while ops fix the email pipeline).
    LOG.info(
        "[password-reset] issued token={} userId={} loginName={} expiresAt={}",
        token.getToken(),
        user.getId(),
        loginName,
        token.getExpiresAt());

    // v0.0.107 (G3) — fire the actual reset email. fault-tolerant: never let an email failure flip
    // the response to 500. token is already persisted above.
    sendResetEmail(user, token.getToken());
  }

  /** Build + send the password-reset email. Swallows all exceptions (logs warn). */
  private void sendResetEmail(User user, String token) {
    String to = user.getEmailAddress();
    if (to == null || to.trim().isEmpty()) {
      // Should not happen — issueResetToken upstream guards on email match. Guard anyway so a future
      // refactor that loosens the upstream check doesn't NPE here.
      LOG.warn("[password-reset] skipping email — user id={} has no emailAddress", user.getId());
      return;
    }
    String link = frontendBaseUrl + "/reset-password?token=" + token;
    String bodyText =
        "您好，\n\n"
            + "请点击下方链接重置 Rainier 账户密码：\n"
            + link
            + "\n\n该链接 1 小时内有效。若非本人操作请忽略此邮件。\n\n— Rainier";
    String bodyHtml =
        "<p>您好，</p>"
            + "<p>请点击下方链接重置 Rainier 账户密码：</p>"
            + "<p><a href=\""
            + link
            + "\">"
            + link
            + "</a></p>"
            + "<p>该链接 1 小时内有效。若非本人操作请忽略此邮件。</p>"
            + "<p>— Rainier</p>";

    EmailMessage msg = new EmailMessage();
    msg.setSubject("Rainier 密码重置");
    List<String> recipients = new ArrayList<String>();
    recipients.add(to.trim());
    msg.setTo(recipients);
    msg.setBodyText(bodyText);
    msg.setBodyHtml(bodyHtml);

    try {
      SendResult r = emailSender.send(msg);
      if (r == null || !r.isSuccess()) {
        LOG.warn(
            "[password-reset] email send returned failure userId={} reason={}",
            user.getId(),
            r == null ? "null result" : r.getErrorMessage());
      }
    } catch (RuntimeException ex) {
      LOG.warn(
          "[password-reset] email send failed userId={} error={}", user.getId(), ex.getMessage());
    }
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
