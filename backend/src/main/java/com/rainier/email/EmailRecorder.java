/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.107 (G3) — 共享的 {@link SentEmailRecord} 落库 + JSON / snippet 工具。 由 {@link LogEmailSender} 与
 * {@link SmtpEmailSender} 复用，避免重复实现。
 */
@Component
public class EmailRecorder {

  /** body 截断长度，超出部分丢弃（持久化用）。 */
  static final int BODY_SNIPPET_MAX = 500;

  private final SentEmailRecordRepository repo;

  public EmailRecorder(SentEmailRecordRepository repo) {
    this.repo = repo;
  }

  /** 成功写一行 status=SENT，返回行 id（providerId 拼接用）。 */
  @Transactional
  public Long recordSent(EmailMessage msg) {
    SentEmailRecord rec = baseRow(msg);
    rec.setStatus(SentEmailRecord.STATUS_SENT);
    rec.setFailReason(null);
    return repo.saveAndFlush(rec).getId();
  }

  /** 失败写一行 status=FAILED，failReason 截断到 500 char。 */
  @Transactional
  public Long recordFailed(EmailMessage msg, String reason) {
    SentEmailRecord rec = baseRow(msg);
    rec.setStatus(SentEmailRecord.STATUS_FAILED);
    rec.setFailReason(snippetReason(reason));
    return repo.saveAndFlush(rec).getId();
  }

  private SentEmailRecord baseRow(EmailMessage msg) {
    SentEmailRecord rec = new SentEmailRecord();
    rec.setFromAddr(msg.getFrom());
    rec.setToAddrsJson(toJsonArray(msg.getTo()));
    rec.setSubject(msg.getSubject());
    rec.setBodyTextSnippet(snippet(msg.getBodyText()));
    rec.setSentAt(LocalDateTime.now());
    return rec;
  }

  /** 极简 JSON array 序列化 —— 转义双引号 + 反斜杠，不引入额外依赖。 */
  static String toJsonArray(List<String> items) {
    if (items == null) {
      return "[]";
    }
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

  private static String snippetReason(String reason) {
    if (reason == null) {
      return null;
    }
    if (reason.length() <= 500) {
      return reason;
    }
    return reason.substring(0, 500);
  }
}
