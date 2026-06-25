/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SMTP {@link EmailSender} 占位 stub（v0.0.92, D4）。仅当 {@code app.email.kind=smtp} 时装配；send
 * 一律抛 {@link UnsupportedOperationException}，等待后续接 JavaMail / SDK 时替换。
 */
@Component
@ConditionalOnProperty(prefix = "app.email", name = "kind", havingValue = "smtp")
public class SmtpEmailSenderStub implements EmailSender {

  @Override
  public SendResult send(EmailMessage msg) {
    throw new UnsupportedOperationException("SmtpEmailSenderStub — not yet implemented");
  }
}
