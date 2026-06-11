/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * ProductModule is the middle level of the product architecture (产品模块). Belongs to one {@link
 * com.rainier.product.domain.Product} (NN FK). Since v0.0.13 modules form a self-referencing tree
 * via {@code parentId} (nullable = top level; max depth configurable, default 3). 3-state machine.
 * {@code productId} immutable after creation; {@code parentId} mutable with strict reparent
 * validation (same product + no cycle + depth cap). Owner mutable. Soft-deleted; bidirectional FK
 * protection on delete (Feature references + child modules).
 *
 * <p>Code uniqueness: composite {@code (parent_id, code)} — the DB constraint skips NULL
 * parent_id rows, so top-level uniqueness within a product is enforced at the service layer.
 */
@Entity
@Table(
    name = "rainier_product_module",
    indexes = @Index(name = "idx_product_module_parent_id", columnList = "parent_id"),
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_product_module_parent_code",
            columnNames = {"parent_id", "code"}))
@SQLDelete(
    sql =
        "UPDATE rainier_product_module SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE"
            + " id = ?")
@Where(clause = "del_flag = 0")
public class ProductModule extends BaseEntity {

  @Column(nullable = false, length = 64)
  private String code;

  @Column(nullable = false, length = 200)
  private String name;

  @Column(length = 4000)
  private String description;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "parent_id")
  private Long parentId;

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

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Long getParentId() {
    return parentId;
  }

  public void setParentId(Long parentId) {
    this.parentId = parentId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }
}
