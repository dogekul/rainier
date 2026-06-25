/* (C) 2026 Rainier — internal use only. */
package com.rainier.metrics.dto;

import java.time.LocalDate;

/** Single overdue project row (v0.0.93, D5). */
public class OverdueProjectRow {

  private Long projectId;
  private String code;
  private String name;
  private String status;
  private Long ownerUserId;
  private LocalDate expectedEndDate; // Project.endDate
  private Long daysOverdue;

  public Long getProjectId() {
    return projectId;
  }

  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public LocalDate getExpectedEndDate() {
    return expectedEndDate;
  }

  public void setExpectedEndDate(LocalDate expectedEndDate) {
    this.expectedEndDate = expectedEndDate;
  }

  public Long getDaysOverdue() {
    return daysOverdue;
  }

  public void setDaysOverdue(Long daysOverdue) {
    this.daysOverdue = daysOverdue;
  }
}
