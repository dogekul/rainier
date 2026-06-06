/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.dto;

import com.rainier.demand.domain.Demand;
import java.time.Instant;

/** Response DTO for {@link Demand} read endpoints. */
public class DemandDetail {

  private Long id;
  private String title;
  private String description;
  private Long submitterUserId;
  private String status;
  private String priority;
  private String source;
  private String aiClassification;
  private Long aiDuplicateHint;
  private String closeReason;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static DemandDetail from(Demand d) {
    DemandDetail dto = new DemandDetail();
    dto.id = d.getId();
    dto.title = d.getTitle();
    dto.description = d.getDescription();
    dto.submitterUserId = d.getSubmitterUserId();
    dto.status = d.getStatus();
    dto.priority = d.getPriority();
    dto.source = d.getSource();
    dto.aiClassification = d.getAiClassification();
    dto.aiDuplicateHint = d.getAiDuplicateHint();
    dto.closeReason = d.getCloseReason();
    dto.createTime = d.getCreateTime();
    dto.updateTime = d.getUpdateTime();
    dto.createBy = d.getCreateBy();
    dto.updateBy = d.getUpdateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public Long getSubmitterUserId() {
    return submitterUserId;
  }

  public String getStatus() {
    return status;
  }

  public String getPriority() {
    return priority;
  }

  public String getSource() {
    return source;
  }

  public String getAiClassification() {
    return aiClassification;
  }

  public Long getAiDuplicateHint() {
    return aiDuplicateHint;
  }

  public String getCloseReason() {
    return closeReason;
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
