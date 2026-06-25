/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

/** Request body for {@code POST /api/field-locks} (v0.0.69, A5). */
public class FieldLockCreateRequest {

  private String entityType;
  private Long entityId;
  private String fieldName;
  private String lockedBy;

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getLockedBy() {
    return lockedBy;
  }

  public void setLockedBy(String lockedBy) {
    this.lockedBy = lockedBy;
  }
}
