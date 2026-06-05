/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

/**
 * Person ↔ organization assignment. Hard delete (no {@code @SQLDelete}); use {@code left_at} for
 * historical tracking.
 *
 * <p>The {@code del_flag} column inherited from {@link BaseEntity} is unused but kept for schema
 * consistency.
 */
@Entity
@Table(name = "rainier_user_organization")
public class UserOrganization extends BaseEntity {

  @Column(name = "user_id", nullable = false, length = 32)
  private String userId;

  @Column(name = "organization_id", nullable = false, length = 32)
  private String organizationId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private UserOrgRole role = UserOrgRole.MEMBER;

  @Column(name = "is_primary", nullable = false)
  private Boolean isPrimary = Boolean.FALSE;

  @Column(name = "joined_at", nullable = false)
  private Instant joinedAt;

  @Column(name = "left_at")
  private Instant leftAt;

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public UserOrgRole getRole() {
    return role;
  }

  public void setRole(UserOrgRole role) {
    this.role = role;
  }

  public Boolean getIsPrimary() {
    return isPrimary;
  }

  public void setIsPrimary(Boolean isPrimary) {
    this.isPrimary = isPrimary;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(Instant joinedAt) {
    this.joinedAt = joinedAt;
  }

  public Instant getLeftAt() {
    return leftAt;
  }

  public void setLeftAt(Instant leftAt) {
    this.leftAt = leftAt;
  }
}
