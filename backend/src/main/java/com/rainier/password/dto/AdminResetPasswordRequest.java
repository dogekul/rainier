/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.dto;

/** Body of {@code POST /api/admin/users/{id}/reset-password}. */
public class AdminResetPasswordRequest {

  private String newPassword;

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
