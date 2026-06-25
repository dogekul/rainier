/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * v0.0.85 (C5) — convenience envelope for {@code GET /api/capability-tags}: both a flat list and a
 * pre-bucketed by-category map (LinkedHashMap so the UI gets a stable order).
 */
public class CapabilityTagListResponse {
  private List<CapabilityTagDto> flat = new ArrayList<>();
  private Map<String, List<CapabilityTagDto>> byCategory = new LinkedHashMap<>();

  public List<CapabilityTagDto> getFlat() {
    return flat;
  }

  public void setFlat(List<CapabilityTagDto> flat) {
    this.flat = flat;
  }

  public Map<String, List<CapabilityTagDto>> getByCategory() {
    return byCategory;
  }

  public void setByCategory(Map<String, List<CapabilityTagDto>> byCategory) {
    this.byCategory = byCategory;
  }
}
