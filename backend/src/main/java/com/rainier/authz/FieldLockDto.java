/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.time.LocalDateTime;

/** Read DTO for {@link FieldLock} (v0.0.69, A5). */
public class FieldLockDto {

  private final Long id;
  private final String entityType;
  private final Long entityId;
  private final String fieldName;
  private final String lockedBy;
  private final LocalDateTime lockedAt;

  public FieldLockDto(
      Long id,
      String entityType,
      Long entityId,
      String fieldName,
      String lockedBy,
      LocalDateTime lockedAt) {
    this.id = id;
    this.entityType = entityType;
    this.entityId = entityId;
    this.fieldName = fieldName;
    this.lockedBy = lockedBy;
    this.lockedAt = lockedAt;
  }

  public static FieldLockDto from(FieldLock l) {
    return new FieldLockDto(
        l.getId(),
        l.getEntityType(),
        l.getEntityId(),
        l.getFieldName(),
        l.getLockedBy(),
        l.getLockedAt());
  }

  public Long getId() {
    return id;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getFieldName() {
    return fieldName;
  }

  public String getLockedBy() {
    return lockedBy;
  }

  public LocalDateTime getLockedAt() {
    return lockedAt;
  }
}
