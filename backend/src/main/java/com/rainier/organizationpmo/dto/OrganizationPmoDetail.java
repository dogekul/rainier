/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.dto;

import com.rainier.organizationpmo.domain.OrganizationPmo;
import java.time.Instant;

/** v0.0.64 — Organization PMO 关系读 DTO（带 enriched user 名/loginName）。 */
public class OrganizationPmoDetail {

  private Long id;
  private Long organizationId;
  private String organizationName;
  private Long userId;
  private String userName;
  private String userLoginName;
  private Instant createTime;

  public static OrganizationPmoDetail from(OrganizationPmo p) {
    OrganizationPmoDetail d = new OrganizationPmoDetail();
    d.id = p.getId();
    d.organizationId = p.getOrganizationId();
    d.userId = p.getUserId();
    d.createTime = p.getCreateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public String getOrganizationName() {
    return organizationName;
  }

  public void setOrganizationName(String organizationName) {
    this.organizationName = organizationName;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getUserLoginName() {
    return userLoginName;
  }

  public void setUserLoginName(String userLoginName) {
    this.userLoginName = userLoginName;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public void setCreateTime(Instant createTime) {
    this.createTime = createTime;
  }
}
