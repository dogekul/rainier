/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Product is the second level of the product architecture (产品). Belongs to one {@link
 * com.rainier.productcategory.domain.ProductCategory} (NN FK). 4-state machine. {@code code}
 * service-unique. {@code categoryId} immutable after creation (v0.0.11 Decision 11 sibling).
 * Owner mutable. Soft-deleted; FK protection on delete (Module references) — wired in M09.
 */
@Entity
@Table(name = "rainier_product")
@SQLDelete(
    sql =
        "UPDATE rainier_product SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Product extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "category_id", nullable = false)
  private Long categoryId;

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

  public Long getCategoryId() {
    return categoryId;
  }

  public void setCategoryId(Long categoryId) {
    this.categoryId = categoryId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }
}
