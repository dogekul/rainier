/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Webhook payload for {@code POST /api/events} (v0.0.65). */
public class EventCreateRequest {

  @NotBlank
  @Size(max = 32)
  private String sourceType;

  @Size(max = 128)
  private String sourceId;

  @NotBlank
  @Size(max = 32)
  private String eventKind;

  /** Raw payload (JSON / text). No size cap here — bounded by HTTP body limit and DB LOB. */
  private String payload;

  @NotNull private LocalDateTime occurredAt;

  public String getSourceType() {
    return sourceType;
  }

  public void setSourceType(String sourceType) {
    this.sourceType = sourceType;
  }

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getEventKind() {
    return eventKind;
  }

  public void setEventKind(String eventKind) {
    this.eventKind = eventKind;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(LocalDateTime occurredAt) {
    this.occurredAt = occurredAt;
  }
}
