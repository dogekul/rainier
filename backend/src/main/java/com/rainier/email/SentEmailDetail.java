/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import java.time.LocalDateTime;

/** v0.0.92 (D4) — {@link SentEmailRecord} read DTO. */
public class SentEmailDetail {

  private Long id;
  private String fromAddr;
  private String toAddrsJson;
  private String subject;
  private String bodyTextSnippet;
  private LocalDateTime sentAt;
  private String status;
  private String failReason;

  public static SentEmailDetail from(SentEmailRecord r) {
    SentEmailDetail d = new SentEmailDetail();
    d.id = r.getId();
    d.fromAddr = r.getFromAddr();
    d.toAddrsJson = r.getToAddrsJson();
    d.subject = r.getSubject();
    d.bodyTextSnippet = r.getBodyTextSnippet();
    d.sentAt = r.getSentAt();
    d.status = r.getStatus();
    d.failReason = r.getFailReason();
    return d;
  }

  public Long getId() {
    return id;
  }

  public String getFromAddr() {
    return fromAddr;
  }

  public String getToAddrsJson() {
    return toAddrsJson;
  }

  public String getSubject() {
    return subject;
  }

  public String getBodyTextSnippet() {
    return bodyTextSnippet;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }

  public String getStatus() {
    return status;
  }

  public String getFailReason() {
    return failReason;
  }
}
