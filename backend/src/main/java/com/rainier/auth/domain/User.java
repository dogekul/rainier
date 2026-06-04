/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.domain;

import java.util.Objects;

/** Minimal user identity carried across the auth flow. v0 has no persistence layer. */
public final class User {

  private final String username;

  public User(String username) {
    this.username = Objects.requireNonNull(username, "username");
  }

  public String getUsername() {
    return username;
  }
}
