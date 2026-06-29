/* (C) 2026 Rainier — internal use only. */
package com.rainier.pmo;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.110 (H3) — PMO company project map. {@code GET /api/pmo/portfolio?groupBy=organization|owner|none}
 * returns company-wide RYG rolled into pivoted groups. Token-gated; same all-users visibility as
 * {@code /api/me/portfolio?scope=all} (the underlying data is identical).
 */
@RestController
@RequestMapping("/api/pmo")
public class PmoPortfolioController {

  private final PmoPortfolioService service;

  public PmoPortfolioController(PmoPortfolioService service) {
    this.service = service;
  }

  @GetMapping(path = "/portfolio", produces = "application/json")
  public List<PmoPortfolioRow> portfolio(
      @RequestParam(name = "groupBy", required = false, defaultValue = "organization") String groupBy,
      HttpServletRequest request) {
    String username = currentUsername(request);
    return service.companyMap(username, PmoPortfolioService.GroupBy.parse(groupBy));
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
