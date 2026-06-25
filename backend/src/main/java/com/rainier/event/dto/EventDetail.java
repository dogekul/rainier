/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.dto;

import com.rainier.event.domain.Event;
import java.time.LocalDateTime;

/** Read DTO for {@link Event} (v0.0.65). */
public class EventDetail {

  private Long id;
  private String sourceType;
  private String sourceId;
  private String eventKind;
  private String payload;
  private LocalDateTime occurredAt;
  private LocalDateTime receivedAt;
  private Boolean processed;
  private String extractedEntityType;
  private Long extractedEntityId;

  public static EventDetail from(Event e) {
    EventDetail d = new EventDetail();
    d.id = e.getId();
    d.sourceType = e.getSourceType();
    d.sourceId = e.getSourceId();
    d.eventKind = e.getEventKind();
    d.payload = e.getPayload();
    d.occurredAt = e.getOccurredAt();
    d.receivedAt = e.getReceivedAt();
    d.processed = e.getProcessed();
    d.extractedEntityType = e.getExtractedEntityType();
    d.extractedEntityId = e.getExtractedEntityId();
    return d;
  }

  public Long getId() {
    return id;
  }

  public String getSourceType() {
    return sourceType;
  }

  public String getSourceId() {
    return sourceId;
  }

  public String getEventKind() {
    return eventKind;
  }

  public String getPayload() {
    return payload;
  }

  public LocalDateTime getOccurredAt() {
    return occurredAt;
  }

  public LocalDateTime getReceivedAt() {
    return receivedAt;
  }

  public Boolean getProcessed() {
    return processed;
  }

  public String getExtractedEntityType() {
    return extractedEntityType;
  }

  public Long getExtractedEntityId() {
    return extractedEntityId;
  }
}
