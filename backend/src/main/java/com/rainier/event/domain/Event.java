/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Raw external event (v0.0.65, flywheel pipeline base). Captures every webhook from GITLAB /
 * DINGTALK / FEISHU / EMAIL / ZENTAO before any business interpretation. {@code payload} stores the
 * untouched JSON; {@link com.rainier.event.extractor.EventExtractor} chain later writes back the
 * extracted internal entity ref. No {@code BaseEntity}: events are append-only stream rows, no soft
 * delete / no audit fields.
 */
@Entity
@Table(name = "rainier_event")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false, updatable = false)
  private Long id;

  /** Source system, e.g. GITLAB / DINGTALK / FEISHU / EMAIL / ZENTAO / MANUAL. */
  @Column(name = "source_type", nullable = false, length = 32)
  private String sourceType;

  /** Optional id in the source system (e.g. GitLab MR id, DingTalk message id). */
  @Column(name = "source_id", length = 128)
  private String sourceId;

  /** Coarse event kind, e.g. COMMIT / PR_OPEN / PR_MERGE / MESSAGE / DOC_CHANGE / OTHER. */
  @Column(name = "event_kind", nullable = false, length = 32)
  private String eventKind;

  /** Raw JSON / text payload as received from the source. */
  @Lob
  @Column(name = "payload")
  private String payload;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @CreationTimestamp
  @Column(name = "received_at", nullable = false, updatable = false)
  private LocalDateTime receivedAt;

  @Column(name = "processed", nullable = false)
  private Boolean processed = Boolean.FALSE;

  /** Filled by the extractor pipeline when an internal entity ref is recognized. */
  @Column(name = "extracted_entity_type", length = 64)
  private String extractedEntityType;

  @Column(name = "extracted_entity_id")
  private Long extractedEntityId;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public LocalDateTime getReceivedAt() {
    return receivedAt;
  }

  public void setReceivedAt(LocalDateTime receivedAt) {
    this.receivedAt = receivedAt;
  }

  public Boolean getProcessed() {
    return processed;
  }

  public void setProcessed(Boolean processed) {
    this.processed = processed;
  }

  public String getExtractedEntityType() {
    return extractedEntityType;
  }

  public void setExtractedEntityType(String extractedEntityType) {
    this.extractedEntityType = extractedEntityType;
  }

  public Long getExtractedEntityId() {
    return extractedEntityId;
  }

  public void setExtractedEntityId(Long extractedEntityId) {
    this.extractedEntityId = extractedEntityId;
  }
}
