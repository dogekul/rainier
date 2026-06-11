/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * v0.0.15 append-only audit trail. One row per successful entity write (create/update/delete),
 * produced by {@code AuditAspect}.
 *
 * <p>Extends {@link BaseEntity} for id + createTime (the audit timestamp); {@code update_*} and
 * {@code del_flag} are inherited but unused — there is no update or delete path (append-only by API
 * contract, no {@code @SQLDelete}).
 *
 * <p>{@code actor} is an explicit column written by the aspect (semantically "who performed the
 * audited action"), distinct from the inherited {@code createBy} even though both resolve to the
 * same {@code AuditorAware} value.
 */
@Entity
@Table(
    name = "rainier_audit_log",
    indexes = {
      @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
      @Index(name = "idx_audit_actor", columnList = "actor")
    })
public class AuditLog extends BaseEntity {

  @Column(name = "actor", length = 32)
  private String actor;

  @Column(name = "entity_type", nullable = false, length = 64)
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @Column(name = "action", nullable = false, length = 16)
  private String action;

  @Column(name = "summary", length = 512)
  private String summary;

  public String getActor() {
    return actor;
  }

  public void setActor(String actor) {
    this.actor = actor;
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

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getSummary() {
    return summary;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
