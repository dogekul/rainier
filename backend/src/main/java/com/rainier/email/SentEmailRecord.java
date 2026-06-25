/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * 邮件发送记录（v0.0.92, D4）。{@link LogEmailSender} 每次 send 写一行。
 *
 * <p>bodyTextSnippet 取前 500 char，避免长正文撑大列。完整正文不持久化（log 即可）。
 */
@Entity
@Table(
    name = "rainier_sent_email",
    indexes = {@Index(name = "idx_sent_email_sent_at", columnList = "sent_at")})
public class SentEmailRecord extends BaseEntity {

  public static final String STATUS_SENT = "SENT";
  public static final String STATUS_FAILED = "FAILED";

  @Column(name = "from_addr", length = 255)
  private String fromAddr;

  /** 收件人 JSON 字符串（["a@x.com","b@y.com"]）。 */
  @Lob
  @Column(name = "to_addrs_json")
  private String toAddrsJson;

  @Column(length = 512)
  private String subject;

  @Column(name = "body_text_snippet", length = 500)
  private String bodyTextSnippet;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "fail_reason", length = 500)
  private String failReason;

  public String getFromAddr() {
    return fromAddr;
  }

  public void setFromAddr(String fromAddr) {
    this.fromAddr = fromAddr;
  }

  public String getToAddrsJson() {
    return toAddrsJson;
  }

  public void setToAddrsJson(String toAddrsJson) {
    this.toAddrsJson = toAddrsJson;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBodyTextSnippet() {
    return bodyTextSnippet;
  }

  public void setBodyTextSnippet(String bodyTextSnippet) {
    this.bodyTextSnippet = bodyTextSnippet;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(LocalDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFailReason() {
    return failReason;
  }

  public void setFailReason(String failReason) {
    this.failReason = failReason;
  }
}
