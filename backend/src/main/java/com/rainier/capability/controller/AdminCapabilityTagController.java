/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.controller;

import com.rainier.capability.dto.CapabilityTagCreateRequest;
import com.rainier.capability.dto.CapabilityTagDto;
import com.rainier.capability.service.CapabilityService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.85 (C5) — admin-only tag creation. Path lives under {@code /api/admin/**} so
 * {@link com.rainier.authz.AdminPaths} Tier A gates the endpoint without touching the path table.
 */
@RestController
@RequestMapping("/api/admin/capability-tags")
public class AdminCapabilityTagController {

  private final CapabilityService service;

  public AdminCapabilityTagController(CapabilityService service) {
    this.service = service;
  }

  @PostMapping(consumes = "application/json", produces = "application/json")
  public CapabilityTagDto create(@RequestBody CapabilityTagCreateRequest req) {
    return service.createTag(req == null ? null : req.getName(), req == null ? null : req.getCategory());
  }
}
