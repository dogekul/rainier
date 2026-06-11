/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.dto;

import com.rainier.productmodule.domain.ProductModule;
import java.time.Instant;

public class ProductModuleDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String status;
  private Long productId;
  private String productCode;
  private String productName;
  private Long parentId;
  private String parentCode;
  private String parentName;
  private String pathName;
  private String pathCodes;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static ProductModuleDetail from(ProductModule m) {
    ProductModuleDetail dto = new ProductModuleDetail();
    dto.id = m.getId();
    dto.code = m.getCode();
    dto.name = m.getName();
    dto.description = m.getDescription();
    dto.status = m.getStatus();
    dto.productId = m.getProductId();
    dto.parentId = m.getParentId();
    dto.ownerUserId = m.getOwnerUserId();
    dto.createTime = m.getCreateTime();
    dto.updateTime = m.getUpdateTime();
    dto.createBy = m.getCreateBy();
    dto.updateBy = m.getUpdateBy();
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
  public Long getProductId() { return productId; }
  public void setProductId(Long productId) { this.productId = productId; }
  public String getProductCode() { return productCode; }
  public void setProductCode(String productCode) { this.productCode = productCode; }
  public String getProductName() { return productName; }
  public void setProductName(String productName) { this.productName = productName; }
  public Long getParentId() { return parentId; }
  public void setParentId(Long parentId) { this.parentId = parentId; }
  public String getParentCode() { return parentCode; }
  public void setParentCode(String parentCode) { this.parentCode = parentCode; }
  public String getParentName() { return parentName; }
  public void setParentName(String parentName) { this.parentName = parentName; }
  public String getPathName() { return pathName; }
  public void setPathName(String pathName) { this.pathName = pathName; }
  public String getPathCodes() { return pathCodes; }
  public void setPathCodes(String pathCodes) { this.pathCodes = pathCodes; }
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
