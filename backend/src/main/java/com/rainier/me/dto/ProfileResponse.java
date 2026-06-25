/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The current user's contribution / capability profile (v0.0.40) — org identity + position + manager
 * + contribution counts. Pure aggregation over UserOrganization / Position / Story / Task; assembled
 * by {@code MeProfileService}.
 */
public class ProfileResponse {

  private Long userId;
  private String loginName;
  private String name;
  private String positionName;
  private String positionCategory;
  private List<Membership> memberships = new ArrayList<>();
  private Manager manager;
  private long ownedStoryCount;
  private long assignedTaskCount;
  private Contribution contribution;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getLoginName() {
    return loginName;
  }

  public void setLoginName(String loginName) {
    this.loginName = loginName;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPositionName() {
    return positionName;
  }

  public void setPositionName(String positionName) {
    this.positionName = positionName;
  }

  public String getPositionCategory() {
    return positionCategory;
  }

  public void setPositionCategory(String positionCategory) {
    this.positionCategory = positionCategory;
  }

  public List<Membership> getMemberships() {
    return memberships;
  }

  public void setMemberships(List<Membership> memberships) {
    this.memberships = memberships;
  }

  public Manager getManager() {
    return manager;
  }

  public void setManager(Manager manager) {
    this.manager = manager;
  }

  public long getOwnedStoryCount() {
    return ownedStoryCount;
  }

  public void setOwnedStoryCount(long ownedStoryCount) {
    this.ownedStoryCount = ownedStoryCount;
  }

  public long getAssignedTaskCount() {
    return assignedTaskCount;
  }

  public void setAssignedTaskCount(long assignedTaskCount) {
    this.assignedTaskCount = assignedTaskCount;
  }

  public Contribution getContribution() {
    return contribution;
  }

  public void setContribution(Contribution contribution) {
    this.contribution = contribution;
  }

  /**
   * v0.0.84 richer-contribution-metrics — detailed activity picture: by-status histograms, current
   * week pulse, and a 4-week trend. Sits beside the legacy {@code ownedStoryCount} /
   * {@code assignedTaskCount} totals which stay for back-compat.
   */
  public static class Contribution {
    private Map<String, Long> tasksByStatus = new LinkedHashMap<>();
    private Map<String, Long> storiesByStatus = new LinkedHashMap<>();
    private long tasksThisWeek;
    private long tasksDoneThisWeek;
    private List<WeekBucket> weeklyTrend = new ArrayList<>();

    public Map<String, Long> getTasksByStatus() {
      return tasksByStatus;
    }

    public void setTasksByStatus(Map<String, Long> tasksByStatus) {
      this.tasksByStatus = tasksByStatus;
    }

    public Map<String, Long> getStoriesByStatus() {
      return storiesByStatus;
    }

    public void setStoriesByStatus(Map<String, Long> storiesByStatus) {
      this.storiesByStatus = storiesByStatus;
    }

    public long getTasksThisWeek() {
      return tasksThisWeek;
    }

    public void setTasksThisWeek(long tasksThisWeek) {
      this.tasksThisWeek = tasksThisWeek;
    }

    public long getTasksDoneThisWeek() {
      return tasksDoneThisWeek;
    }

    public void setTasksDoneThisWeek(long tasksDoneThisWeek) {
      this.tasksDoneThisWeek = tasksDoneThisWeek;
    }

    public List<WeekBucket> getWeeklyTrend() {
      return weeklyTrend;
    }

    public void setWeeklyTrend(List<WeekBucket> weeklyTrend) {
      this.weeklyTrend = weeklyTrend;
    }
  }

  /** One ISO-week bucket in {@link Contribution#weeklyTrend}. */
  public static class WeekBucket {
    private final String week;
    private final long tasksDone;
    private final long storiesDone;

    public WeekBucket(String week, long tasksDone, long storiesDone) {
      this.week = week;
      this.tasksDone = tasksDone;
      this.storiesDone = storiesDone;
    }

    public String getWeek() {
      return week;
    }

    public long getTasksDone() {
      return tasksDone;
    }

    public long getStoriesDone() {
      return storiesDone;
    }
  }

  /** One active org membership of the user. */
  public static class Membership {
    private final Long organizationId;
    private final String organizationName;
    private final String organizationType;
    private final String role;
    private final boolean isPrimary;

    public Membership(
        Long organizationId,
        String organizationName,
        String organizationType,
        String role,
        boolean isPrimary) {
      this.organizationId = organizationId;
      this.organizationName = organizationName;
      this.organizationType = organizationType;
      this.role = role;
      this.isPrimary = isPrimary;
    }

    public Long getOrganizationId() {
      return organizationId;
    }

    public String getOrganizationName() {
      return organizationName;
    }

    public String getOrganizationType() {
      return organizationType;
    }

    public String getRole() {
      return role;
    }

    public boolean getIsPrimary() {
      return isPrimary;
    }
  }

  /** The user's direct manager (first non-self active HEAD walking up from the primary org). */
  public static class Manager {
    private final Long userId;
    private final String name;
    private final String loginName;

    public Manager(Long userId, String name, String loginName) {
      this.userId = userId;
      this.name = name;
      this.loginName = loginName;
    }

    public Long getUserId() {
      return userId;
    }

    public String getName() {
      return name;
    }

    public String getLoginName() {
      return loginName;
    }
  }
}
