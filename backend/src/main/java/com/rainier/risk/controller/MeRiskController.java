/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.risk.RiskFinding;
import com.rainier.risk.RiskService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 风险雷达 read-model (v0.0.70). {@code GET /api/me/risks?scope=mine} returns the merged finding
 * list across every registered {@link com.rainier.risk.RiskRule} for the caller's project scope.
 * All-users / token-gated (not admin-gated) — same posture as {@code /api/me/inbox}.
 */
@RestController
@RequestMapping("/api/me")
public class MeRiskController {

  private final RiskService riskService;

  public MeRiskController(RiskService riskService) {
    this.riskService = riskService;
  }

  @GetMapping(path = "/risks", produces = "application/json")
  public List<RiskFinding> risks(
      HttpServletRequest request,
      @RequestParam(name = "scope", required = false, defaultValue = "mine") String scope) {
    return riskService.runAll(currentUsername(request), scope);
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
