/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * v0.0.107 (G3) — 真实 SMTP {@link EmailSender}，替换原 {@code SmtpEmailSenderStub}。
 *
 * <p>仅当 {@code app.email.kind=smtp} 时装配。底层走 Spring Boot 的 {@link JavaMailSender}（来自
 * {@code spring-boot-starter-mail}），把 {@link EmailMessage} 转成 {@link SimpleMailMessage}（纯文本）。
 * HTML 邮件模板不在本切片范围。
 *
 * <p>发送失败（{@link MailException}）不抛 —— 落一行 {@code STATUS_FAILED} 的 {@link SentEmailRecord} +
 * 返回 {@link SendResult#failure(String)}，调用方决定是否容错。
 */
@Component
@ConditionalOnProperty(prefix = "app.email", name = "kind", havingValue = "smtp")
public class SmtpEmailSender implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(SmtpEmailSender.class);

  private final JavaMailSender mailSender;
  private final EmailRecorder recorder;

  public SmtpEmailSender(JavaMailSender mailSender, EmailRecorder recorder) {
    this.mailSender = mailSender;
    this.recorder = recorder;
  }

  @Override
  public SendResult send(EmailMessage msg) {
    if (msg == null) {
      return SendResult.failure("EmailMessage is null");
    }
    List<String> to = msg.getTo();
    if (to == null || to.isEmpty()) {
      return SendResult.failure("at least one recipient required");
    }

    SimpleMailMessage mime = new SimpleMailMessage();
    if (msg.getFrom() != null) {
      mime.setFrom(msg.getFrom());
    }
    mime.setTo(to.toArray(new String[0]));
    List<String> cc = msg.getCc();
    if (cc != null && !cc.isEmpty()) {
      mime.setCc(cc.toArray(new String[0]));
    }
    mime.setSubject(msg.getSubject());
    mime.setText(msg.getBodyText() == null ? "" : msg.getBodyText());

    try {
      mailSender.send(mime);
    } catch (MailException ex) {
      LOG.warn("[email/smtp] send failed: {}", ex.getMessage());
      Long id = recorder.recordFailed(msg, ex.getMessage());
      return SendResult.failure("smtp:" + id + ":" + ex.getMessage());
    }

    LOG.info(
        "[email/smtp] sent from={} to={} subject={} bodyLen={}",
        msg.getFrom(),
        EmailRecorder.toJsonArray(to),
        msg.getSubject(),
        msg.getBodyText() == null ? 0 : msg.getBodyText().length());
    Long id = recorder.recordSent(msg);
    return SendResult.success("smtp:" + id);
  }
}
