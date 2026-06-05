/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.dto;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import java.time.Instant;

/** Response DTO for organization read endpoints. */
public class OrganizationDetail {

  private Long id;
  private Long parentId;
  private OrganizationType type;
  private String code;
  private String name;
  private String description;
  private String path;
  private String wholeName;
  private Boolean isPmo;
  private Boolean enabled;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static OrganizationDetail from(Organization o) {
    OrganizationDetail dto = new OrganizationDetail();
    dto.id = o.getId();
    dto.parentId = o.getParentId();
    dto.type = o.getType();
    dto.code = o.getCode();
    dto.name = o.getName();
    dto.description = o.getDescription();
    dto.path = o.getPath();
    dto.wholeName = o.getWholeName();
    dto.isPmo = o.getIsPmo();
    dto.enabled = o.getEnabled();
    dto.createTime = o.getCreateTime();
    dto.updateTime = o.getUpdateTime();
    dto.createBy = o.getCreateBy();
    dto.updateBy = o.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public Long getParentId() {
    return parentId;
  }

  public OrganizationType getType() {
    return type;
  }

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getPath() {
    return path;
  }

  public String getWholeName() {
    return wholeName;
  }

  public Boolean getIsPmo() {
    return isPmo;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }

  public String getCreateBy() {
    return createBy;
  }

  public String getUpdateBy() {
    return updateBy;
  }
}
