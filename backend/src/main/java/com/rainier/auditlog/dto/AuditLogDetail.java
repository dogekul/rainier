/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.dto;

import com.rainier.auditlog.domain.AuditLog;
import java.time.Instant;

/** Read DTO for {@link AuditLog}. */
public class AuditLogDetail {

  private Long id;
  private String actor;
  private String entityType;
  private Long entityId;
  private String action;
  private String summary;
  private Instant createTime;

  public static AuditLogDetail from(AuditLog a) {
    AuditLogDetail dto = new AuditLogDetail();
    dto.id = a.getId();
    dto.actor = a.getActor();
    dto.entityType = a.getEntityType();
    dto.entityId = a.getEntityId();
    dto.action = a.getAction();
    dto.summary = a.getSummary();
    dto.createTime = a.getCreateTime();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public String getActor() {
    return actor;
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

  public String getSummary() {
    return summary;
  }

  public Instant getCreateTime() {
    return createTime;
  }
}
