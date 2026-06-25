/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.fulllink;

/**
 * One step on the 全链 timeline — a stage code with display label, current marker, and activity /
 * artifact counts (v0.0.94 D6).
 */
public class StageSummary {

  private String code;
  private String label;
  private boolean current;
  private int activityCount;
  private int doneCount;
  private int artifactCount;

  public StageSummary() {}

  public StageSummary(
      String code,
      String label,
      boolean current,
      int activityCount,
      int doneCount,
      int artifactCount) {
    this.code = code;
    this.label = label;
    this.current = current;
    this.activityCount = activityCount;
    this.doneCount = doneCount;
    this.artifactCount = artifactCount;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isCurrent() {
    return current;
  }

  public void setCurrent(boolean current) {
    this.current = current;
  }

  public int getActivityCount() {
    return activityCount;
  }

  public void setActivityCount(int activityCount) {
    this.activityCount = activityCount;
  }

  public int getDoneCount() {
    return doneCount;
  }

  public void setDoneCount(int doneCount) {
    this.doneCount = doneCount;
  }

  public int getArtifactCount() {
    return artifactCount;
  }

  public void setArtifactCount(int artifactCount) {
    this.artifactCount = artifactCount;
  }
}
