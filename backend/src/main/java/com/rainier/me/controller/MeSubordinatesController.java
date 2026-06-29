/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.dto.Subordinate;
import com.rainier.me.service.MeSubordinatesService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.111 (H4) — 我的下属面板入口端点. {@code GET /api/me/subordinates} returns the direct subordinates
 * of the caller (active members of orgs the caller HEADs, one level, excluding self). Token-gated,
 * NOT admin-gated; non-HEAD callers get an empty list.
 */
@RestController
@RequestMapping("/api/me")
public class MeSubordinatesController {

  private final MeSubordinatesService service;

  public MeSubordinatesController(MeSubordinatesService service) {
    this.service = service;
  }

  @GetMapping(path = "/subordinates", produces = "application/json")
  public List<Subordinate> subordinates(HttpServletRequest request) {
    return service.subordinatesOf(currentUsername(request));
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
