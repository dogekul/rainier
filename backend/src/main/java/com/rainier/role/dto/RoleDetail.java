/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.dto;

import com.rainier.role.domain.Role;
import java.time.Instant;

/** Response DTO for {@link Role} read endpoints. */
public class RoleDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private Boolean enabled;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static RoleDetail from(Role r) {
    RoleDetail dto = new RoleDetail();
    dto.id = r.getId();
    dto.code = r.getCode();
    dto.name = r.getName();
    dto.description = r.getDescription();
    dto.enabled = r.getEnabled();
    dto.createTime = r.getCreateTime();
    dto.updateTime = r.getUpdateTime();
    dto.createBy = r.getCreateBy();
    dto.updateBy = r.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
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
