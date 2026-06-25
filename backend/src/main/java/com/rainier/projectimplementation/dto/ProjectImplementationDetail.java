/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation.dto;

import com.rainier.projectimplementation.domain.ProjectImplementation;
import java.time.Instant;

/** Read DTO for {@link ProjectImplementation} (v0.0.89). */
public class ProjectImplementationDetail {

  private Long id;
  private Long projectId;
  private String scopeMarkdown;
  private Integer estimatedManDays;
  private String riskNotes;
  private String keyMilestonesJson;
  private Instant createTime;
  private Instant updateTime;

  public static ProjectImplementationDetail from(ProjectImplementation p) {
    ProjectImplementationDetail d = new ProjectImplementationDetail();
    d.id = p.getId();
    d.projectId = p.getProjectId();
    d.scopeMarkdown = p.getScopeMarkdown();
    d.estimatedManDays = p.getEstimatedManDays();
    d.riskNotes = p.getRiskNotes();
    d.keyMilestonesJson = p.getKeyMilestonesJson();
    d.createTime = p.getCreateTime();
    d.updateTime = p.getUpdateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public Long getProjectId() {
    return projectId;
  }

  public String getScopeMarkdown() {
    return scopeMarkdown;
  }

  public Integer getEstimatedManDays() {
    return estimatedManDays;
  }

  public String getRiskNotes() {
    return riskNotes;
  }

  public String getKeyMilestonesJson() {
    return keyMilestonesJson;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }
}
