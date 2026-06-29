/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

/**
 * 邮件发送抽象（v0.0.92, D4）。默认装配 {@link LogEmailSender}（不真发，只 log + 落库）； {@code
 * app.email.kind=smtp} 时装配 {@link SmtpEmailSender}（v0.0.107 G3，走 {@code JavaMailSender} 真发）。
 */
public interface EmailSender {

  /** 发送一条邮件。实现负责持久化 {@link SentEmailRecord}（如有需要）。 */
  SendResult send(EmailMessage msg);
}
