/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/opportunities/{id}/initiate} (v0.0.44) — the 立项评审 gate handing a WON
 * opportunity into a delivery Project. {@code decision} (PASS/REJECT) validated in the service.
 *
 * <p>v0.0.48: 立项 PASS 可「关联或新建 对外-交付 项目」二选一 —— 传 {@code projectId}（须为 EXTERNAL_DELIVERY）
 * 关联已有，或传 {@code projectCode}/{@code projectName}（可选 {@code projectOwnerUserId}，默认商机 pmUserId）
 * 内联新建一个 EXTERNAL_DELIVERY 项目。校验在 service（二者缺一/兼有 → 400）。
 */
public class OpportunityInitiateRequest {

  /** Link an existing delivery project (must be EXTERNAL_DELIVERY). Optional since v0.0.48. */
  private Long projectId;

  // v0.0.48 — inline-create a delivery project (used when projectId is absent).
  @Size(max = 64)
  private String projectCode;

  @Size(max = 200)
  private String projectName;

  private Long projectOwnerUserId;

  @Size(max = 16)
  private String decision;

  @Size(max = 500)
  private String note;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getProjectCode() {
    return projectCode;
  }

  public void setProjectCode(String projectCode) {
    this.projectCode = projectCode;
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public Long getProjectOwnerUserId() {
    return projectOwnerUserId;
  }

  public void setProjectOwnerUserId(Long projectOwnerUserId) {
    this.projectOwnerUserId = projectOwnerUserId;
  }

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
