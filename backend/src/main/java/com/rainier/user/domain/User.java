/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Person identity. Minimal: login + name + work-number + email + flags + audit.
 *
 * <p>Soft delete enabled — see {@link com.rainier.common.persistence.BaseEntity} and design.md §6.
 */
@Entity
// v0.0.38: NO DB unique on login_name — a plain unique index conflicts with the soft-delete model
// (@SQLDelete keeps the row with del_flag=1, so the index would block recreating a login after delete).
// Uniqueness is enforced at the service layer via existsByLoginName, which honors @Where(del_flag=0).
@Table(name = "rainier_user")
@SQLDelete(
    sql = "UPDATE rainier_user SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class User extends BaseEntity {

  @Column(name = "login_name", nullable = false, length = 30)
  private String loginName;

  @Column(nullable = false, length = 30)
  private String name;

  @Column(length = 30)
  private String code;

  @Column(name = "email_address", length = 255)
  private String emailAddress;

  @Column(name = "is_internal", nullable = false)
  private Boolean isInternal = Boolean.TRUE;

  @Column(nullable = false)
  private Boolean enabled = Boolean.TRUE;

  /** v0.0.7: optional FK to {@code rainier_position(id)} — see entity-user MODIFIED spec. */
  @Column(name = "position_id")
  private Long positionId;

  /** v0.0.38: BCrypt password hash. Nullable (legacy rows backfilled at startup); never serialized. */
  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  public String getLoginName() {
    return loginName;
  }

  public void setLoginName(String loginName) {
    this.loginName = loginName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public Boolean getIsInternal() {
    return isInternal;
  }

  public void setIsInternal(Boolean isInternal) {
    this.isInternal = isInternal;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Long getPositionId() {
    return positionId;
  }

  public void setPositionId(Long positionId) {
    this.positionId = positionId;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }
}
