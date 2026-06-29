/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 默认 {@link EmailSender} 实现（v0.0.92, D4）。仅 log + 持久化 {@link SentEmailRecord}，不真发。
 *
 * <p>v0.0.107 (G3) — 落库逻辑外移到 {@link EmailRecorder}，与 {@link SmtpEmailSender} 共用。
 *
 * <p>{@code @Primary} + {@code matchIfMissing=true}：除非显式把 {@code app.email.kind=smtp}，否则一直
 * 是它生效。
 */
@Component
@Primary
@ConditionalOnProperty(
    prefix = "app.email",
    name = "kind",
    havingValue = "log",
    matchIfMissing = true)
public class LogEmailSender implements EmailSender {

  private static final Logger LOG = LoggerFactory.getLogger(LogEmailSender.class);

  private final EmailRecorder recorder;

  public LogEmailSender(EmailRecorder recorder) {
    this.recorder = recorder;
  }

  @Override
  @Transactional
  public SendResult send(EmailMessage msg) {
    if (msg == null) {
      return SendResult.failure("EmailMessage is null");
    }
    List<String> to = msg.getTo();
    if (to == null || to.isEmpty()) {
      return SendResult.failure("at least one recipient required");
    }
    LOG.info(
        "[email/log] from={} to={} subject={} bodyLen={}",
        msg.getFrom(),
        EmailRecorder.toJsonArray(to),
        msg.getSubject(),
        msg.getBodyText() == null ? 0 : msg.getBodyText().length());

    Long id = recorder.recordSent(msg);
    return SendResult.success("log:" + id);
  }
}
