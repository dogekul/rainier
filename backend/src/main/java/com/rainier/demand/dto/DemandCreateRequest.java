/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/demands}. AI fields are intentionally excluded — they are written
 * only by AI workers, not by clients (design.md decision 3).
 */
public class DemandCreateRequest {

  @NotBlank
  @Size(max = 100)
  private String title;

  @Size(max = 2000)
  private String description;

  @NotNull private Long submitterUserId;

  @Size(max = 16)
  private String status;

  @Size(max = 16)
  private String priority;

  @Size(max = 16)
  private String source;

  @Size(max = 500)
  private String closeReason;

  /** v0.0.56 — 可选来源商机 id（非空则后端校验存在）。 */
  private Long opportunityId;

  public Long getOpportunityId() {
    return opportunityId;
  }

  public void setOpportunityId(Long opportunityId) {
    this.opportunityId = opportunityId;
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

  public Long getSubmitterUserId() {
    return submitterUserId;
  }

  public void setSubmitterUserId(Long submitterUserId) {
    this.submitterUserId = submitterUserId;
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

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }
}
