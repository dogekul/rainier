/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.89 — Project 立项后的「施工内容」结构化记录（1:1，按 {@code projectId} 唯一）。
 *
 * <p>承载 scopeMarkdown / estimatedManDays / riskNotes / keyMilestonesJson 等字段，配合
 * `/api/projects/{id}/implementation` 上行 upsert 端点。
 */
@Entity
@Table(
    name = "rainier_project_implementation",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_project_implementation_project",
          columnNames = {"project_id"})
    })
@SQLDelete(
    sql =
        "UPDATE rainier_project_implementation SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class ProjectImplementation extends BaseEntity {

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Lob
  @Column(name = "scope_markdown", nullable = false)
  private String scopeMarkdown;

  @Column(name = "estimated_man_days")
  private Integer estimatedManDays;

  @Column(name = "risk_notes", length = 2000)
  private String riskNotes;

  @Lob
  @Column(name = "key_milestones_json")
  private String keyMilestonesJson;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getScopeMarkdown() {
    return scopeMarkdown;
  }

  public void setScopeMarkdown(String scopeMarkdown) {
    this.scopeMarkdown = scopeMarkdown;
  }

  public Integer getEstimatedManDays() {
    return estimatedManDays;
  }

  public void setEstimatedManDays(Integer estimatedManDays) {
    this.estimatedManDays = estimatedManDays;
  }

  public String getRiskNotes() {
    return riskNotes;
  }

  public void setRiskNotes(String riskNotes) {
    this.riskNotes = riskNotes;
  }

  public String getKeyMilestonesJson() {
    return keyMilestonesJson;
  }

  public void setKeyMilestonesJson(String keyMilestonesJson) {
    this.keyMilestonesJson = keyMilestonesJson;
  }
}
