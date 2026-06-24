/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.dto;

import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/requirements}.
 *
 * <p>{@code sourceDemandIds} is OPTIONAL. When present and non-empty, the service creates the
 * requirement and N rows of {@code rainier_demand_requirement} (linkType="DERIVED") atomically; any
 * unknown demand id causes the whole transaction to roll back (workflow-demand-conversion
 * capability — see design.md decision 4).
 */
public class RequirementCreateRequest {

  /** v0.0.56 — 可空：缺省则后端按自增 id 生成 REQ-{id}（参照 Project v0.0.49 模式）。 */
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String title;

  @Size(max = 4000)
  private String description;

  @NotNull private Long ownerUserId;

  @Size(max = 16)
  private String status;

  @Size(max = 16)
  private String priority;

  @Size(max = 8)
  private String complexity;

  private Long projectId;

  @Size(max = 500)
  private String closeReason;

  /** v0.0.19 — 期望交付日期 (可空). */
  private LocalDate expectedDate;

  private List<Long> sourceDemandIds;

  /** v0.0.56 — 可选来源商机 id（非空则后端校验存在）。 */
  private Long opportunityId;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getComplexity() {
    return complexity;
  }

  public void setComplexity(String complexity) {
    this.complexity = complexity;
  }

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }

  public LocalDate getExpectedDate() {
    return expectedDate;
  }

  public void setExpectedDate(LocalDate expectedDate) {
    this.expectedDate = expectedDate;
  }

  public List<Long> getSourceDemandIds() {
    return sourceDemandIds;
  }

  public void setSourceDemandIds(List<Long> sourceDemandIds) {
    this.sourceDemandIds = sourceDemandIds;
  }
}
