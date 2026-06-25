/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.dto;

import com.rainier.capability.domain.CapabilityTag;

/** v0.0.85 (C5) — read-model for {@link CapabilityTag}. */
public class CapabilityTagDto {
  private Long id;
  private String name;
  private String category;

  public static CapabilityTagDto from(CapabilityTag t) {
    CapabilityTagDto d = new CapabilityTagDto();
    d.id = t.getId();
    d.name = t.getName();
    d.category = t.getCategory();
    return d;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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
