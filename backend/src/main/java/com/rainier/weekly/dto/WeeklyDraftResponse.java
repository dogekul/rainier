/* (C) 2026 Rainier — internal use only. */
package com.rainier.weekly.dto;

import com.rainier.weekly.domain.WeeklyDraft;
import java.time.Instant;
import java.time.LocalDate;

/** API view of {@link WeeklyDraft}. */
public class WeeklyDraftResponse {
  private Long id;
  private Long userId;
  private LocalDate periodStart;
  private LocalDate periodEnd;
  private String contentMarkdown;
  private String status;
  private Instant createdAt;
  private Instant acceptedAt;

  public static WeeklyDraftResponse from(WeeklyDraft d) {
    WeeklyDraftResponse r = new WeeklyDraftResponse();
    r.id = d.getId();
    r.userId = d.getUserId();
    r.periodStart = d.getPeriodStart();
    r.periodEnd = d.getPeriodEnd();
    r.contentMarkdown = d.getContentMarkdown();
    r.status = d.getStatus();
    r.createdAt = d.getCreatedAt();
    r.acceptedAt = d.getAcceptedAt();
    return r;
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public String getContentMarkdown() {
    return contentMarkdown;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getAcceptedAt() {
    return acceptedAt;
  }
}
