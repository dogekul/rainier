/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.dto;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.user.domain.User;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import java.time.Instant;

/**
 * Read DTO for {@link UserOrganization}, enriched with the related user's name and the related
 * organization's name + type so the front-end need not issue extra requests.
 */
public class UserOrgDetail {

  private String id;
  private String userId;
  private String userLoginName;
  private String userName;
  private String organizationId;
  private String organizationName;
  private OrganizationType organizationType;
  private UserOrgRole role;
  private Boolean isPrimary;
  private Instant joinedAt;
  private Instant leftAt;
  private Instant createTime;
  private Instant updateTime;

  public static UserOrgDetail from(UserOrganization uo, User u, Organization o) {
    UserOrgDetail d = new UserOrgDetail();
    d.id = uo.getId();
    d.userId = uo.getUserId();
    d.userLoginName = u == null ? null : u.getLoginName();
    d.userName = u == null ? null : u.getName();
    d.organizationId = uo.getOrganizationId();
    d.organizationName = o == null ? null : o.getName();
    d.organizationType = o == null ? null : o.getType();
    d.role = uo.getRole();
    d.isPrimary = uo.getIsPrimary();
    d.joinedAt = uo.getJoinedAt();
    d.leftAt = uo.getLeftAt();
    d.createTime = uo.getCreateTime();
    d.updateTime = uo.getUpdateTime();
    return d;
  }

  public String getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public String getUserLoginName() {
    return userLoginName;
  }

  public String getUserName() {
    return userName;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public OrganizationType getOrganizationType() {
    return organizationType;
  }

  public UserOrgRole getRole() {
    return role;
  }

  public Boolean getIsPrimary() {
    return isPrimary;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public Instant getLeftAt() {
    return leftAt;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }
}
