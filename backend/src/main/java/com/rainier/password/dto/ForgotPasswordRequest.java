/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.dto;

/** Body of {@code POST /api/auth/forgot-password}. */
public class ForgotPasswordRequest {

  private String loginName;
  private String email;

  public String getLoginName() {
    return loginName;
  }

  public void setLoginName(String loginName) {
    this.loginName = loginName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
