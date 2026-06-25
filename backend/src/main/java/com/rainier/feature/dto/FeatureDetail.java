/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.dto;

import com.rainier.feature.domain.Feature;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

public class FeatureDetail {

  private Long id;
  private String code;
  private String name;
  private String description;
  private String status;
  private Long moduleId;
  private String moduleCode;
  private String moduleName;
  private Long ownerUserId;
  private String ownerName;
  private String ownerLoginName;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;
  /** v0.0.86 (C6) — directly linked Requirement ids. */
  private List<Long> requirementIds = Collections.emptyList();

  public static FeatureDetail from(Feature f) {
    FeatureDetail dto = new FeatureDetail();
    dto.id = f.getId();
    dto.code = f.getCode();
    dto.name = f.getName();
    dto.description = f.getDescription();
    dto.status = f.getStatus();
    dto.moduleId = f.getModuleId();
    dto.ownerUserId = f.getOwnerUserId();
    dto.createTime = f.getCreateTime();
    dto.updateTime = f.getUpdateTime();
    dto.createBy = f.getCreateBy();
    dto.updateBy = f.getUpdateBy();
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
  public Long getModuleId() { return moduleId; }
  public void setModuleId(Long moduleId) { this.moduleId = moduleId; }
  public String getModuleCode() { return moduleCode; }
  public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }
  public String getModuleName() { return moduleName; }
  public void setModuleName(String moduleName) { this.moduleName = moduleName; }
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
  public List<Long> getRequirementIds() { return requirementIds; }
  public void setRequirementIds(List<Long> requirementIds) {
    this.requirementIds = requirementIds == null ? Collections.<Long>emptyList() : requirementIds;
  }
}
