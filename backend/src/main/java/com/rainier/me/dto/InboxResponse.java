/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The PO「需求收件箱」read-model (v0.0.42): unconverted demands awaiting triage + the caller's own
 * requirements. Self-scoped, all-users; assembled by {@code MeInboxService}.
 */
public class InboxResponse {

  private List<InboxDemand> unconvertedDemands = new ArrayList<>();
  private List<InboxRequirement> myRequirements = new ArrayList<>();

  public List<InboxDemand> getUnconvertedDemands() {
    return unconvertedDemands;
  }

  public void setUnconvertedDemands(List<InboxDemand> unconvertedDemands) {
    this.unconvertedDemands = unconvertedDemands;
  }

  public List<InboxRequirement> getMyRequirements() {
    return myRequirements;
  }

  public void setMyRequirements(List<InboxRequirement> myRequirements) {
    this.myRequirements = myRequirements;
  }

  /** A demand with no requirement link yet — a PO triage item. */
  public static class InboxDemand {
    private final Long id;
    private final String title;
    private final String priority;
    private final String status;
    private final Instant createTime;

    public InboxDemand(Long id, String title, String priority, String status, Instant createTime) {
      this.id = id;
      this.title = title;
      this.priority = priority;
      this.status = status;
      this.createTime = createTime;
    }

    public Long getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public String getPriority() {
      return priority;
    }

    public String getStatus() {
      return status;
    }

    public Instant getCreateTime() {
      return createTime;
    }
  }

  /** A requirement owned by the caller. */
  public static class InboxRequirement {
    private final Long id;
    private final String code;
    private final String title;
    private final String status;
    private final String priority;
    private final LocalDate expectedDate;
    private final Long projectId;
    private final String projectName;

    public InboxRequirement(
        Long id,
        String code,
        String title,
        String status,
        String priority,
        LocalDate expectedDate,
        Long projectId,
        String projectName) {
      this.id = id;
      this.code = code;
      this.title = title;
      this.status = status;
      this.priority = priority;
      this.expectedDate = expectedDate;
      this.projectId = projectId;
      this.projectName = projectName;
    }

    public Long getId() {
      return id;
    }

    public String getCode() {
      return code;
    }

    public String getTitle() {
      return title;
    }

    public String getStatus() {
      return status;
    }

    public String getPriority() {
      return priority;
    }

    public LocalDate getExpectedDate() {
      return expectedDate;
    }

    public Long getProjectId() {
      return projectId;
    }

    public String getProjectName() {
      return projectName;
    }
  }
}
