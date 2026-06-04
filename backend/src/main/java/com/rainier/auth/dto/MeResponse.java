/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.dto;

/** Current-user payload returned by {@code GET /api/auth/me}. */
public class MeResponse {

  private final String username;

  public MeResponse(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
}
