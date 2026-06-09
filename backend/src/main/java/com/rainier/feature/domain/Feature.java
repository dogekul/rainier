/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Feature is the leaf of the product architecture (功能). Belongs to one
 * {@link com.rainier.productmodule.domain.ProductModule} (NN FK). 3-state machine.
 * {@code moduleId} immutable after creation. Owner mutable. Soft-deleted; no downstream FK
 * protection (leaf entity).
 */
@Entity
@Table(name = "rainier_feature")
@SQLDelete(
    sql =
        "UPDATE rainier_feature SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Feature extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "module_id", nullable = false)
  private Long moduleId;

  @Column(name = "owner_user_id", nullable = false)
  private Long ownerUserId;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getModuleId() {
    return moduleId;
  }

  public void setModuleId(Long moduleId) {
    this.moduleId = moduleId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }
}
