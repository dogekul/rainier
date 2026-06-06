/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.dto;

import com.rainier.position.domain.Position;
import java.time.Instant;

/** Response DTO for {@link Position} read endpoints. */
public class PositionDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String category;
  private Boolean enabled;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static PositionDetail from(Position p) {
    PositionDetail dto = new PositionDetail();
    dto.id = p.getId();
    dto.code = p.getCode();
    dto.name = p.getName();
    dto.description = p.getDescription();
    dto.category = p.getCategory();
    dto.enabled = p.getEnabled();
    dto.createTime = p.getCreateTime();
    dto.updateTime = p.getUpdateTime();
    dto.createBy = p.getCreateBy();
    dto.updateBy = p.getUpdateBy();
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

  public String getCategory() {
    return category;
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
