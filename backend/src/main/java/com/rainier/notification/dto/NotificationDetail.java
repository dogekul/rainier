/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification.dto;

import com.rainier.notification.domain.Notification;
import java.time.LocalDateTime;

/** Read-projection for {@link Notification}（v0.0.72, A8）。 */
public class NotificationDetail {

  private Long id;
  private Long userId;
  private String title;
  private String body;
  private String level;
  private String entityType;
  private Long entityId;
  private LocalDateTime createdAt;
  private LocalDateTime readAt;

  public NotificationDetail() {
    // for Jackson
  }

  public static NotificationDetail from(Notification n) {
    NotificationDetail d = new NotificationDetail();
    d.id = n.getId();
    d.userId = n.getUserId();
    d.title = n.getTitle();
    d.body = n.getBody();
    d.level = n.getLevel();
    d.entityType = n.getEntityType();
    d.entityId = n.getEntityId();
    d.createdAt = n.getCreatedAt();
    d.readAt = n.getReadAt();
    return d;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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
