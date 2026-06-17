/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.dto.LedTeam;
import com.rainier.me.dto.TeamMember;
import com.rainier.me.service.MeTeamService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-scoped team endpoints for the team-lead panel (v0.0.24). Identity is the token subject
 * resolved by {@code SecurityFilter} (token-gated, NOT admin-gated): any authenticated user may ask
 * which teams THEY lead, but {@code team-members} re-checks HEAD membership server-side and 403s
 * otherwise.
 */
@RestController
@RequestMapping("/api/me")
public class MeTeamController {

  private final MeTeamService service;

  public MeTeamController(MeTeamService service) {
    this.service = service;
  }

  @GetMapping(path = "/led-teams", produces = "application/json")
  public List<LedTeam> ledTeams(HttpServletRequest request) {
    return service.ledTeams(currentUsername(request));
  }

  @GetMapping(path = "/team-members", produces = "application/json")
  public List<TeamMember> teamMembers(
      @RequestParam Long organizationId, HttpServletRequest request) {
    return service.teamMembers(currentUsername(request), organizationId);
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
