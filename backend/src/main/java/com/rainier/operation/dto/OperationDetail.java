/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.dto;

import com.rainier.operation.domain.Operation;
import java.time.Instant;

/** Read DTO for {@link Operation} (v0.0.44). */
public class OperationDetail {

  private Long id;
  private String customerName;
  private String title;
  private String stage;
  private String status;
  private Long opsOwnerUserId;
  private String opsOwnerName;
  private Long projectId;
  private Instant createTime;
  private Instant updateTime;

  public static OperationDetail from(Operation o) {
    OperationDetail d = new OperationDetail();
    d.id = o.getId();
    d.customerName = o.getCustomerName();
    d.title = o.getTitle();
    d.stage = o.getStage();
    d.status = o.getStatus();
    d.opsOwnerUserId = o.getOpsOwnerUserId();
    d.projectId = o.getProjectId();
    d.createTime = o.getCreateTime();
    d.updateTime = o.getUpdateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getTitle() {
    return title;
  }

  public String getStage() {
    return stage;
  }

  public String getStatus() {
    return status;
  }

  public Long getOpsOwnerUserId() {
    return opsOwnerUserId;
  }

  public String getOpsOwnerName() {
    return opsOwnerName;
  }

  public void setOpsOwnerName(String opsOwnerName) {
    this.opsOwnerName = opsOwnerName;
  }

  public Long getProjectId() {
    return projectId;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }
}
