/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Position pool — formal job title attached to a User as a single-valued label.
 *
 * <p>Uniqueness on {@code code} is enforced at the service layer (matching Organization.code /
 * Requirement.code patterns) — DB-level unique would leak soft-delete residue and block legal
 * re-insertion.
 */
@Entity
@Table(name = "rainier_position")
@SQLDelete(
    sql =
        "UPDATE rainier_position SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Position extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @Column(nullable = false, length = 16)
  private String category;

  @Column(nullable = false)
  private Boolean enabled = Boolean.TRUE;

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

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }
}
