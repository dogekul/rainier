/* (C) 2026 Rainier — internal use only. */
package com.rainier.password;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rainier.email.EmailMessage;
import com.rainier.email.EmailSender;
import com.rainier.email.SendResult;
import com.rainier.password.repository.PasswordResetTokenRepository;
import com.rainier.password.service.PasswordService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * v0.0.107 (G3) — verify {@link PasswordService#issueResetToken} actually fires an email via {@link
 * EmailSender} (not just the legacy INFO log).
 */
@SpringBootTest
@ActiveProfiles("test")
class PasswordServiceEmailIntegrationTest {

  @Autowired private PasswordService service;
  @Autowired private UserRepository userRepo;
  @Autowired private PasswordResetTokenRepository tokenRepo;
  @Autowired private PasswordEncoder encoder;

  @MockBean private EmailSender emailSender;

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

    when(emailSender.send(any(EmailMessage.class))).thenReturn(SendResult.success("mock:1"));
  }

  /** Scenario 1 — happy path: send invoked once, body contains reset link + token. */
  @Test
  void forgotPassword_sendsEmail_withTokenInBody() {
    service.issueResetToken("alice", "alice@example.com");

    ArgumentCaptor<EmailMessage> captor = ArgumentCaptor.forClass(EmailMessage.class);
    verify(emailSender, times(1)).send(captor.capture());

    EmailMessage msg = captor.getValue();
    assertThat(msg.getTo()).containsExactly("alice@example.com");
    assertThat(msg.getSubject()).isEqualTo("Rainier 密码重置");

    String issuedToken = tokenRepo.findAll().get(0).getToken();
    assertThat(msg.getBodyText())
        .contains("http://localhost/reset-password?token=" + issuedToken)
        .contains("1 小时内有效");
    assertThat(msg.getBodyHtml())
        .contains("href=\"http://localhost/reset-password?token=" + issuedToken + "\"");

    // token persisted
    assertThat(tokenRepo.count()).isEqualTo(1L);
  }

  /** Scenario 2 — email mismatch (carol has null email) → silent no-op, EmailSender NOT invoked. */
  @Test
  void forgotPassword_emailMismatch_skipsEmail_andNoToken() {
    User carol = new User();
    carol.setLoginName("carol");
    carol.setName("Carol");
    carol.setEmailAddress(null);
    carol.setIsInternal(true);
    carol.setEnabled(true);
    carol.setPasswordHash(encoder.encode("s3cretPW"));
    userRepo.saveAndFlush(carol);

    service.issueResetToken("carol", "anything@example.com");

    verify(emailSender, never()).send(any(EmailMessage.class));
    assertThat(tokenRepo.count()).isEqualTo(0L);
  }

  /** Scenario 3 — EmailSender throws → forgotPassword still returns; token persisted. */
  @Test
  void forgotPassword_emailSenderThrows_isSwallowed_andTokenPersisted() {
    when(emailSender.send(any(EmailMessage.class)))
        .thenThrow(new RuntimeException("smtp down"));

    assertThatCode(() -> service.issueResetToken("alice", "alice@example.com"))
        .doesNotThrowAnyException();

    assertThat(tokenRepo.count()).isEqualTo(1L);
    verify(emailSender, times(1)).send(any(EmailMessage.class));
  }
}
