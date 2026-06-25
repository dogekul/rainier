/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.85 (C5) — global capability tag dictionary (TECH / PRODUCT / SOFT). Uniqueness on
 * {@code name} is enforced at the service layer (sibling pattern of {@code Position.code}) so soft
 * deletes don't block legal re-inserts.
 */
@Entity
@Table(name = "rainier_capability_tag")
@SQLDelete(
    sql =
        "UPDATE rainier_capability_tag SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class CapabilityTag extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String name;

  @Column(nullable = false, length = 16)
  private String category;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
}
