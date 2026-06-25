/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.capability.dto.UserCapabilityDto;
import com.rainier.capability.dto.UserCapabilitySetRequest;
import com.rainier.capability.service.CapabilityService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.85 (C5) — self-scoped capability assessment. {@code source} is always SELF here; the
 * manager-assessment endpoint is intentionally out of scope for this slice.
 */
@RestController
@RequestMapping("/api/me/capabilities")
public class MeCapabilityController {

  private final CapabilityService service;
  private final UserRepository userRepo;

  public MeCapabilityController(CapabilityService service, UserRepository userRepo) {
    this.service = service;
    this.userRepo = userRepo;
  }

  @GetMapping(produces = "application/json")
  public List<UserCapabilityDto> mine(HttpServletRequest request) {
    User me = currentUser(request);
    if (me == null) {
      // Token valid but no matching user — return empty (mirrors degraded /api/me/profile).
      return Collections.emptyList();
    }
    return service.listUserCapabilities(me.getId());
  }

  @PostMapping(consumes = "application/json", produces = "application/json")
  public UserCapabilityDto setMine(
      @RequestBody UserCapabilitySetRequest req, HttpServletRequest request) {
    User me = currentUser(request);
    if (me == null) {
      throw new ForbiddenException("Caller has no matching user record");
    }
    return service.setUserCapability(
        me.getId(),
        req == null ? null : req.getCapabilityTagId(),
        req == null ? null : req.getLevel(),
        "SELF");
  }

  private User currentUser(HttpServletRequest request) {
    String username = currentUsername(request);
    return userRepo.findByLoginName(username).orElse(null);
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
