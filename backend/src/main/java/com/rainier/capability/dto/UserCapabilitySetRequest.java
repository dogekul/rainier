/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.dto;

/** v0.0.85 (C5) — body for {@code POST /api/me/capabilities}. {@code source} is forced to SELF. */
public class UserCapabilitySetRequest {
  private Long capabilityTagId;
  private Integer level;

  public Long getCapabilityTagId() {
    return capabilityTagId;
  }

  public void setCapabilityTagId(Long capabilityTagId) {
    this.capabilityTagId = capabilityTagId;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(Integer level) {
    this.level = level;
  }
}
