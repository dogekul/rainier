/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.dto;

/** Body of {@code POST /api/me/password}. */
public class ChangePasswordRequest {

  private String currentPassword;
  private String newPassword;

  public String getCurrentPassword() {
    return currentPassword;
  }

  public void setCurrentPassword(String currentPassword) {
    this.currentPassword = currentPassword;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
