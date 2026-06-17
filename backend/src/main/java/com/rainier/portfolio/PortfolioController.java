/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Scoped project-health portfolio (v0.0.28). {@code GET /api/me/portfolio?scope=mine|led|all} returns
 * per-project open/overdue/blocked + RYG for the caller's scope. Token-gated (identity from
 * SecurityFilter); the reusable read-model the cross-project cockpit / team panel / company map consume.
 */
@RestController
@RequestMapping("/api/me")
public class PortfolioController {

  private final ScopeService scopeService;
  private final PortfolioService portfolioService;

  public PortfolioController(ScopeService scopeService, PortfolioService portfolioService) {
    this.scopeService = scopeService;
    this.portfolioService = portfolioService;
  }

  @GetMapping(path = "/portfolio", produces = "application/json")
  public List<PortfolioRow> portfolio(
      @RequestParam(defaultValue = "mine") String scope, HttpServletRequest request) {
    String username = currentUsername(request);
    List<Long> projectIds = scopeService.resolveProjectIds(username, scope);
    return portfolioService.portfolio(projectIds);
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
