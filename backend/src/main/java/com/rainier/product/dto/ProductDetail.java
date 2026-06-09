/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.dto;

import com.rainier.product.domain.Product;
import java.time.Instant;

public class ProductDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String status;
  private Long categoryId;
  private String categoryCode;
  private String categoryName;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static ProductDetail from(Product p) {
    ProductDetail dto = new ProductDetail();
    dto.id = p.getId();
    dto.code = p.getCode();
    dto.name = p.getName();
    dto.description = p.getDescription();
    dto.status = p.getStatus();
    dto.categoryId = p.getCategoryId();
    dto.ownerUserId = p.getOwnerUserId();
    dto.createTime = p.getCreateTime();
    dto.updateTime = p.getUpdateTime();
    dto.createBy = p.getCreateBy();
    dto.updateBy = p.getUpdateBy();
    return dto;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Long getCategoryId() { return categoryId; }
  public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
  public String getCategoryCode() { return categoryCode; }
  public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
  public String getCategoryName() { return categoryName; }
  public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
  public Long getOwnerUserId() { return ownerUserId; }
  public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
  public String getOwnerName() { return ownerName; }
  public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
  public String getOwnerLoginName() { return ownerLoginName; }
  public void setOwnerLoginName(String ownerLoginName) { this.ownerLoginName = ownerLoginName; }
  public Instant getCreateTime() { return createTime; }
  public void setCreateTime(Instant createTime) { this.createTime = createTime; }
  public Instant getUpdateTime() { return updateTime; }
  public void setUpdateTime(Instant updateTime) { this.updateTime = updateTime; }
  public String getCreateBy() { return createBy; }
  public void setCreateBy(String createBy) { this.createBy = createBy; }
  public String getUpdateBy() { return updateBy; }
  public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
