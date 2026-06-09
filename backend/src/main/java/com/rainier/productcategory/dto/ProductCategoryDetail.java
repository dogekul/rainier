/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.dto;

import com.rainier.productcategory.domain.ProductCategory;
import java.time.Instant;

public class ProductCategoryDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String status;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static ProductCategoryDetail from(ProductCategory c) {
    ProductCategoryDetail dto = new ProductCategoryDetail();
    dto.id = c.getId();
    dto.code = c.getCode();
    dto.name = c.getName();
    dto.description = c.getDescription();
    dto.status = c.getStatus();
    dto.ownerUserId = c.getOwnerUserId();
    dto.createTime = c.getCreateTime();
    dto.updateTime = c.getUpdateTime();
    dto.createBy = c.getCreateBy();
    dto.updateBy = c.getUpdateBy();
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
