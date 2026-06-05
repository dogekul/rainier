/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Adjacency-list node in the organization tree.
 *
 * <p>Soft delete: {@code @SQLDelete} converts repository {@code .delete()} into {@code UPDATE
 * del_flag=1} and {@code @Where} filters all queries to non-deleted rows. Native queries bypass
 * both (used by tests and rare maintenance scripts).
 */
@Entity
@Table(name = "rainier_organization")
@SQLDelete(
    sql =
        "UPDATE rainier_organization SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id"
            + " = ?")
@Where(clause = "del_flag = 0")
public class Organization extends BaseEntity {

  @Column(name = "parent_id")
  private Long parentId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private OrganizationType type;

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(length = 200)
  private String path;

  @Column(name = "whole_name", length = 500)
  private String wholeName;

  @Column(name = "is_pmo", nullable = false)
  private Boolean isPmo = Boolean.FALSE;

  @Column(nullable = false)
  private Boolean enabled = Boolean.TRUE;

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public OrganizationType getType() {
    return type;
  }

  public void setType(OrganizationType type) {
    this.type = type;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getWholeName() {
    return wholeName;
  }

  public void setWholeName(String wholeName) {
    this.wholeName = wholeName;
  }

  public Boolean getIsPmo() {
    return isPmo;
  }

  public void setIsPmo(Boolean isPmo) {
    this.isPmo = isPmo;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
