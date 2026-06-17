/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.dto;

/** A team (organization) the current user is the HEAD of (v0.0.24). */
public class LedTeam {

  private final Long organizationId;
  private final String organizationName;
  private final String organizationType;

  public LedTeam(Long organizationId, String organizationName, String organizationType) {
    this.organizationId = organizationId;
    this.organizationName = organizationName;
    this.organizationType = organizationType;
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
}
