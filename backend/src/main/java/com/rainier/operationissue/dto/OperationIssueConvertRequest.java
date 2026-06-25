/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.dto;

import javax.validation.constraints.NotNull;

/** v0.0.95 — convert-to-task 请求体。仅需 projectId，title/description 从 issue 复制。 */
public class OperationIssueConvertRequest {

  @NotNull private Long projectId;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }
}
