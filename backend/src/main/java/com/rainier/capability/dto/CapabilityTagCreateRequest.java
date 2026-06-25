/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.dto;

/** v0.0.85 (C5) — body for {@code POST /api/admin/capability-tags}. */
public class CapabilityTagCreateRequest {
  private String name;
  private String category;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }
}
