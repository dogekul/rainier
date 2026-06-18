/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.dto.ProfileResponse;
import com.rainier.me.service.MeProfileService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-scoped personal profile (v0.0.40). {@code GET /api/me/profile} returns the caller's org
 * identity + position + direct manager + contribution counts (identity from {@code SecurityFilter};
 * token-gated, NOT admin-gated). The read-model behind the「我的档案」landing page.
 */
@RestController
@RequestMapping("/api/me")
public class MeProfileController {

  private final MeProfileService service;

  public MeProfileController(MeProfileService service) {
    this.service = service;
  }

  @GetMapping(path = "/profile", produces = "application/json")
  public ProfileResponse profile(HttpServletRequest request) {
    return service.profileOf(currentUsername(request));
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
