/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.31 关联面板 — a minimal external-artifact link attached to a Story or Task. Deliberately tiny
 * (targetType+targetId, linkType, label, url) per the role cards' repeated 「关联必须极简否则没人维护」
 * trap. This is also the evidence base the future AI Agent's {@code evidence_refs} will sit on.
 */
@Entity
@Table(name = "rainier_entity_link")
@SQLDelete(
    sql =
        "UPDATE rainier_entity_link SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id ="
            + " ?")
@Where(clause = "del_flag = 0")
public class EntityLink extends BaseEntity {

  @Column(name = "target_type", nullable = false, length = 16)
  private String targetType;

  @Column(name = "target_id", nullable = false)
  private Long targetId;

  @Column(name = "link_type", nullable = false, length = 16)
  private String linkType;

  @Column(length = 200)
  private String label;

  @Column(nullable = false, length = 1000)
  private String url;

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  public String getLinkType() {
    return linkType;
  }

  public void setLinkType(String linkType) {
    this.linkType = linkType;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
