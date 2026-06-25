/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import java.time.LocalDateTime;
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

  /** body 截断长度，超出部分丢弃（持久化用）。 */
  static final int BODY_SNIPPET_MAX = 500;

  private final SentEmailRecordRepository repo;

  public LogEmailSender(SentEmailRecordRepository repo) {
    this.repo = repo;
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
    String toJson = toJsonArray(to);
    LOG.info(
        "[email/log] from={} to={} subject={} bodyLen={}",
        msg.getFrom(),
        toJson,
        msg.getSubject(),
        msg.getBodyText() == null ? 0 : msg.getBodyText().length());

    SentEmailRecord rec = new SentEmailRecord();
    rec.setFromAddr(msg.getFrom());
    rec.setToAddrsJson(toJson);
    rec.setSubject(msg.getSubject());
    rec.setBodyTextSnippet(snippet(msg.getBodyText()));
    rec.setSentAt(LocalDateTime.now());
    rec.setStatus(SentEmailRecord.STATUS_SENT);
    rec.setFailReason(null);
    rec = repo.saveAndFlush(rec);
    return SendResult.success("log:" + rec.getId());
  }

  /** 极简 JSON array 序列化 —— 转义双引号 + 反斜杠，不引入额外依赖。 */
  static String toJsonArray(List<String> items) {
    StringBuilder sb = new StringBuilder(2 + items.size() * 16);
    sb.append('[');
    for (int i = 0; i < items.size(); i++) {
      if (i > 0) {
        sb.append(',');
      }
      sb.append('"');
      String s = items.get(i) == null ? "" : items.get(i);
      for (int j = 0; j < s.length(); j++) {
        char c = s.charAt(j);
        if (c == '\\' || c == '"') {
          sb.append('\\').append(c);
        } else if (c == '\n') {
          sb.append("\\n");
        } else if (c == '\r') {
          sb.append("\\r");
        } else if (c == '\t') {
          sb.append("\\t");
        } else {
          sb.append(c);
        }
      }
      sb.append('"');
    }
    sb.append(']');
    return sb.toString();
  }

  static String snippet(String body) {
    if (body == null) {
      return null;
    }
    if (body.length() <= BODY_SNIPPET_MAX) {
      return body;
    }
    return body.substring(0, BODY_SNIPPET_MAX);
  }
}
