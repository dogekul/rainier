/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * v0.0.107 (G3) — verify {@link SmtpEmailSender} translates {@link EmailMessage} into a {@link
 * SimpleMailMessage} and is fault-tolerant on {@link MailSendException}.
 *
 * <p>Pure unit — no Spring context, no real SMTP relay.
 */
class SmtpEmailSenderTest {

  @Test
  void send_happyPath_callsMailSenderAndReturnsSuccess() {
    JavaMailSender mail = mock(JavaMailSender.class);
    EmailRecorder recorder = mock(EmailRecorder.class);
    org.mockito.Mockito.when(recorder.recordSent(any(EmailMessage.class))).thenReturn(42L);
    SmtpEmailSender sender = new SmtpEmailSender(mail, recorder);

    EmailMessage msg = new EmailMessage();
    msg.setFrom("noreply@rainier.local");
    msg.setTo(Arrays.asList("a@x.com"));
    msg.setSubject("hi");
    msg.setBodyText("hello world");

    SendResult r = sender.send(msg);

    ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mail, times(1)).send(captor.capture());
    SimpleMailMessage mime = captor.getValue();
    assertThat(mime.getFrom()).isEqualTo("noreply@rainier.local");
    assertThat(mime.getTo()).containsExactly("a@x.com");
    assertThat(mime.getSubject()).isEqualTo("hi");
    assertThat(mime.getText()).isEqualTo("hello world");

    assertThat(r.isSuccess()).isTrue();
    assertThat(r.getProviderId()).isEqualTo("smtp:42");
    verify(recorder, times(1)).recordSent(msg);
  }

  @Test
  void send_mailExceptionThrown_isSwallowed_andFailedRowPersisted() {
    JavaMailSender mail = mock(JavaMailSender.class);
    EmailRecorder recorder = mock(EmailRecorder.class);
    org.mockito.Mockito.when(recorder.recordFailed(any(EmailMessage.class), any(String.class)))
        .thenReturn(99L);
    doThrow(new MailSendException("relay down")).when(mail).send(any(SimpleMailMessage.class));
    SmtpEmailSender sender = new SmtpEmailSender(mail, recorder);

    EmailMessage msg = new EmailMessage();
    msg.setTo(Arrays.asList("a@x.com"));
    msg.setSubject("s");
    msg.setBodyText("b");

    SendResult r = assertThatCodeReturns(sender, msg);
    assertThat(r.isSuccess()).isFalse();
    assertThat(r.getErrorMessage()).contains("relay down");
    verify(recorder, times(1)).recordFailed(any(EmailMessage.class), any(String.class));
  }

  @Test
  void send_emptyRecipients_returnsFailureWithoutTouchingMailSender() {
    JavaMailSender mail = mock(JavaMailSender.class);
    EmailRecorder recorder = mock(EmailRecorder.class);
    SmtpEmailSender sender = new SmtpEmailSender(mail, recorder);

    EmailMessage msg = new EmailMessage();
    msg.setTo(java.util.Collections.<String>emptyList());

    SendResult r = sender.send(msg);
    assertThat(r.isSuccess()).isFalse();
    verify(mail, org.mockito.Mockito.never()).send(any(SimpleMailMessage.class));
    verify(recorder, org.mockito.Mockito.never()).recordSent(any(EmailMessage.class));
  }

  /** small wrapper so a MailException-throwing send still returns its SendResult cleanly. */
  private static SendResult assertThatCodeReturns(SmtpEmailSender sender, EmailMessage msg) {
    final SendResult[] holder = new SendResult[1];
    assertThatCode(() -> holder[0] = sender.send(msg)).doesNotThrowAnyException();
    return holder[0];
  }
}
