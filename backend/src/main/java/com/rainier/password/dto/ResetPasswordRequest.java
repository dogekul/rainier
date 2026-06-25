/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.dto;

/** Body of {@code POST /api/auth/reset-password}. */
public class ResetPasswordRequest {

  private String token;
  private String newPassword;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
