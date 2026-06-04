/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.dto;

/** Outbound user shape exposed by the auth API. */
public class UserDto {

  private final String username;

  public UserDto(String username) {
    this.username = username;
  }

  public String getUsername() {
    return username;
  }
}
