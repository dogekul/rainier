/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.idp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a successful {@link IdentityProvider#authenticate}. Cross-provider VO — does
 * NOT map to a JPA entity. v0.0.75 B2.
 */
public final class UserIdentity {

  private final String externalId;
  private final String loginName;
  private final String displayName;
  private final String email;
  private final List<String> groups;

  public UserIdentity(
      String externalId, String loginName, String displayName, String email, List<String> groups) {
    this.externalId = externalId;
    this.loginName = loginName;
    this.displayName = displayName;
    this.email = email;
    this.groups =
        (groups == null)
            ? Collections.<String>emptyList()
            : Collections.unmodifiableList(new ArrayList<String>(groups));
  }

  public String getExternalId() {
    return externalId;
  }

  public String getLoginName() {
    return loginName;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmail() {
    return email;
  }

  public List<String> getGroups() {
    return groups;
  }
}
