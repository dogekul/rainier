/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * v0.0.85 (C5) — user × capability tag with self-assessed (or manager-assessed) level 1..5.
 *
 * <p>Hard delete (no {@code @SQLDelete}/{@code @Where}) — same lifecycle posture as
 * {@code UserOrganization}: removing a capability means really gone, not soft-archived.
 *
 * <p>{@code (user_id, capability_tag_id)} is unique so an upsert can dedupe without an extra query.
 */
@Entity
@Table(
    name = "rainier_user_capability",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_user_capability_user_tag",
            columnNames = {"user_id", "capability_tag_id"}))
public class UserCapability extends BaseEntity {

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "capability_tag_id", nullable = false)
  private Long capabilityTagId;

  @Column(nullable = false)
  private Integer level;

  @Column(nullable = false, length = 16)
  private String source;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getCapabilityTagId() {
    return capabilityTagId;
  }

  public void setCapabilityTagId(Long capabilityTagId) {
    this.capabilityTagId = capabilityTagId;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(Integer level) {
    this.level = level;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
