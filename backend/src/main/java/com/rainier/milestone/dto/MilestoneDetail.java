/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.dto;

import com.rainier.milestone.domain.Milestone;
import java.time.Instant;
import java.time.LocalDate;

/** Response DTO for {@link Milestone} read endpoints. */
public class MilestoneDetail {

  private Long id;
  private Long projectId;
  private String code;
  private String name;
  private String description;
  private LocalDate targetDate;
  private String status;
  private LocalDate actualDate;
  private Integer sortOrder;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static MilestoneDetail from(Milestone m) {
    MilestoneDetail dto = new MilestoneDetail();
    dto.id = m.getId();
    dto.projectId = m.getProjectId();
    dto.code = m.getCode();
    dto.name = m.getName();
    dto.description = m.getDescription();
    dto.targetDate = m.getTargetDate();
    dto.status = m.getStatus();
    dto.actualDate = m.getActualDate();
    dto.sortOrder = m.getSortOrder();
    dto.createTime = m.getCreateTime();
    dto.updateTime = m.getUpdateTime();
    dto.createBy = m.getCreateBy();
    dto.updateBy = m.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public Long getProjectId() {
    return projectId;
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

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public String getStatus() {
    return status;
  }

  public LocalDate getActualDate() {
    return actualDate;
  }

  public Integer getSortOrder() {
    return sortOrder;
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
