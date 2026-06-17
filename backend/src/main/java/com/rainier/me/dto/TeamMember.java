/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

/** An active member of a team the current user HEADs (v0.0.24). */
public class TeamMember {

  private final Long userId;
  private final String name;
  private final String loginName;

  public TeamMember(Long userId, String name, String loginName) {
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
