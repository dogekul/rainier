/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.dto;

/** Login success payload. */
public class LoginResponse {

  private final String token;
  private final UserDto user;

  public LoginResponse(String token, UserDto user) {
    this.token = token;
    this.user = user;
  }

  public String getToken() {
    return token;
  }

  public UserDto getUser() {
    return user;
  }
}
