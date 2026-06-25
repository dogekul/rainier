/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.dto;

/**
 * v0.0.64 — 「Effective PMO」= 某组织的有效 PMO 列表条目。包含 own + 沿 parent_id 链向上继承的全部 PMOs；
 * `inheritedFromOrgId` 标示该条目来自哪个组织（自身 = 当前 org id；继承 = 祖先 org id）。
 */
public class EffectivePmoDetail {

  private Long userId;
  private String userName;
  private String userLoginName;
  private Long inheritedFromOrgId;
  private String inheritedFromOrgName;

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

  public Long getInheritedFromOrgId() {
    return inheritedFromOrgId;
  }

  public void setInheritedFromOrgId(Long inheritedFromOrgId) {
    this.inheritedFromOrgId = inheritedFromOrgId;
  }

  public String getInheritedFromOrgName() {
    return inheritedFromOrgName;
  }

  public void setInheritedFromOrgName(String inheritedFromOrgName) {
    this.inheritedFromOrgName = inheritedFromOrgName;
  }
}
