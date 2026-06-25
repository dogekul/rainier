/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

/**
 * v0.0.69 (A5): 单字段写保护。即使用户开了 DEPTH 授权，AI Agent 在改字段前也必须查这张表 —— 若已
 * 登记锁，写入被拒绝。本版仅落库 + 查询，不在写路径强制（A6/A7 才查锁）。
 *
 * <p>非软删 —— unlock 直接物理 delete（锁是瞬态状态，无审计需求）。
 */
@Entity
@Table(
    name = "rainier_field_lock",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_field_lock_entity_field",
          columnNames = {"entity_type", "entity_id", "field_name"})
    })
public class FieldLock {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(nullable = false, updatable = false)
  private Long id;

  @Column(name = "entity_type", nullable = false, length = 32)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @Column(name = "field_name", nullable = false, length = 64)
  private String fieldName;

  /** 锁来源：USER / AI / ADMIN。 */
  @Column(name = "locked_by", nullable = false, length = 16)
  private String lockedBy;

  @CreationTimestamp
  @Column(name = "locked_at", nullable = false, updatable = false)
  private LocalDateTime lockedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public LocalDateTime getLockedAt() {
    return lockedAt;
  }

  public void setLockedAt(LocalDateTime lockedAt) {
    this.lockedAt = lockedAt;
  }
}
