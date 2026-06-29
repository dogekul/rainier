/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * v0.0.92 (D4) — {@link LogEmailSender} 行为验证：send 即落库 + body 截断 + 边界。
 *
 * <p>SmtpEmailSenderStub 走 unit 静态构造（不需要 spring context）。
 */
@SpringBootTest
@ActiveProfiles("test")
class LogEmailSenderTest {

  @Autowired private EmailSender emailSender;
  @Autowired private SentEmailRecordRepository repo;

  @BeforeEach
  void clean() {
    repo.deleteAll();
  }

  /** Scenario 1 — send 写入 SentEmailRecord 一行，status=SENT，sentAt 非空。 */
  @Test
  void send_persistsRecordWithSentStatus() {
    EmailMessage msg = new EmailMessage();
    msg.setFrom("noreply@rainier.local");
    msg.setTo(Arrays.asList("alice@x.com"));
    msg.setSubject("hi");
    msg.setBodyText("hello world");

    SendResult r = emailSender.send(msg);
    assertThat(r.isSuccess()).isTrue();
    assertThat(r.getProviderId()).startsWith("log:");

    List<SentEmailRecord> rows = repo.findAll();
    assertThat(rows).hasSize(1);
    SentEmailRecord row = rows.get(0);
    assertThat(row.getFromAddr()).isEqualTo("noreply@rainier.local");
    assertThat(row.getToAddrsJson()).contains("alice@x.com");
    assertThat(row.getSubject()).isEqualTo("hi");
    assertThat(row.getBodyTextSnippet()).isEqualTo("hello world");
    assertThat(row.getStatus()).isEqualTo(SentEmailRecord.STATUS_SENT);
    assertThat(row.getSentAt()).isNotNull();
    assertThat(row.getFailReason()).isNull();
  }

  /** Scenario 2 — bodyText > 500 → 截断为前 500 char。 */
  @Test
  void send_truncatesBodyText_to500Chars() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 800; i++) {
      sb.append('a');
    }
    String original = sb.toString();
    EmailMessage msg = new EmailMessage();
    msg.setTo(Arrays.asList("a@x.com"));
    msg.setSubject("s");
    msg.setBodyText(original);

    emailSender.send(msg);
    SentEmailRecord row = repo.findAll().get(0);
    assertThat(row.getBodyTextSnippet()).hasSize(500);
    assertThat(row.getBodyTextSnippet()).isEqualTo(original.substring(0, 500));
  }

  /** Scenario 3 — 空 recipients → failure，不落库。 */
  @Test
  void send_emptyRecipients_returnsFailureAndNoRow() {
    EmailMessage msg = new EmailMessage();
    msg.setTo(Collections.<String>emptyList());
    msg.setSubject("s");
    msg.setBodyText("b");

    SendResult r = emailSender.send(msg);
    assertThat(r.isSuccess()).isFalse();
    assertThat(r.getErrorMessage()).isNotNull();
    assertThat(repo.count()).isEqualTo(0L);
  }

  /** Scenario 5 — 多收件人 JSON 序列化正确。 */
  @Test
  void send_multipleRecipients_jsonHasAll() {
    EmailMessage msg = new EmailMessage();
    msg.setTo(Arrays.asList("a@x.com", "b@y.com", "c@z.com"));
    msg.setSubject("s");
    msg.setBodyText("b");
    emailSender.send(msg);
    String json = repo.findAll().get(0).getToAddrsJson();
    assertThat(json).contains("a@x.com").contains("b@y.com").contains("c@z.com");
    assertThat(json).startsWith("[").endsWith("]");
  }
}
