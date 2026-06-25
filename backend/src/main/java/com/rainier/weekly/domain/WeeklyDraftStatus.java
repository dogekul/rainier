/* (C) 2026 Rainier — internal use only. */
package com.rainier.weekly.domain;

/** 3-state machine for {@link WeeklyDraft}. */
public final class WeeklyDraftStatus {
  public static final String DRAFT = "DRAFT";
  public static final String ACCEPTED = "ACCEPTED";
  public static final String SENT = "SENT";

  private WeeklyDraftStatus() {}
}
