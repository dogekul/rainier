/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * 站内通知（v0.0.72, A8）。"被动看板 → 主动推送" 飞轮的第一站：仅站内通知，邮件/IM 走后续 stub。
 *
 * <p>level 沿用 RiskFinding 的 String 约定（INFO/WARN/CRIT），不引入 enum；readAt 用 timestamp 表达"已读"。
 */
@Entity
@Table(
    name = "rainier_notification",
    indexes = {
      @Index(name = "idx_notif_user", columnList = "user_id"),
      @Index(name = "idx_notif_user_read", columnList = "user_id, read_at")
    })
public class Notification extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(nullable = false, length = 256)
  private String title;

  @Lob
  @Column
  private String body;

  /** INFO / WARN / CRIT，与 {@code RiskFinding.level} 对齐。 */
  @Column(nullable = false, length = 16)
  private String level;

  @Column(name = "entity_type", length = 64)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /** 非空表示已读。 */
  @Column(name = "read_at")
  private LocalDateTime readAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = level;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getReadAt() {
    return readAt;
  }

  public void setReadAt(LocalDateTime readAt) {
    this.readAt = readAt;
  }
}
