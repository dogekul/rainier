/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.extractor;

/**
 * Result of an {@link EventExtractor#extract} call: which internal entity an external event maps
 * to, and the intended action keyword (e.g. "UPDATE", "CLOSE", "NOTE"). Pure value object.
 */
public class ExtractionResult {

  private final String entityType;
  private final Long entityId;
  private final String action;

  public ExtractionResult(String entityType, Long entityId, String action) {
    this.entityType = entityType;
    this.entityId = entityId;
    this.action = action;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getAction() {
    return action;
  }
}
