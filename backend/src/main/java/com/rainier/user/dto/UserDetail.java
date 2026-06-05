/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.dto;

import com.rainier.user.domain.User;
import java.time.Instant;

/** Response DTO for user read endpoints. */
public class UserDetail {

  private String id;
  private String loginName;
  private String name;
  private String code;
  private String emailAddress;
  private Boolean isInternal;
  private Boolean enabled;
  private Boolean delFlag;
  private Instant createTime;
  private Instant updateTime;
  private String createBy;
  private String updateBy;

  public static UserDetail from(User u) {
    UserDetail d = new UserDetail();
    d.id = u.getId();
    d.loginName = u.getLoginName();
    d.name = u.getName();
    d.code = u.getCode();
    d.emailAddress = u.getEmailAddress();
    d.isInternal = u.getIsInternal();
    d.enabled = u.getEnabled();
    d.delFlag = u.getDelFlag();
    d.createTime = u.getCreateTime();
    d.updateTime = u.getUpdateTime();
    d.createBy = u.getCreateBy();
    d.updateBy = u.getUpdateBy();
    return d;
  }

  public String getId() {
    return id;
  }

  public String getLoginName() {
    return loginName;
  }

  public String getName() {
    return name;
  }

  public String getCode() {
    return code;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public Boolean getIsInternal() {
    return isInternal;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public Boolean getDelFlag() {
    return delFlag;
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
