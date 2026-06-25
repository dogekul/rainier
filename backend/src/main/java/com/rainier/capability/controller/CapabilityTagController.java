/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.controller;

import com.rainier.capability.dto.CapabilityTagListResponse;
import com.rainier.capability.service.CapabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.85 (C5) — all-users read of the capability tag dictionary. Token-optional in the default
 * security profile (mirrors {@code /api/positions} GET behaviour); admin tier handles writes via
 * {@link AdminCapabilityTagController}.
 */
@RestController
@RequestMapping("/api/capability-tags")
public class CapabilityTagController {

  private final CapabilityService service;

  public CapabilityTagController(CapabilityService service) {
    this.service = service;
  }

  @GetMapping(produces = "application/json")
  public CapabilityTagListResponse list() {
    return service.listAllTagsWithBuckets();
  }
}
