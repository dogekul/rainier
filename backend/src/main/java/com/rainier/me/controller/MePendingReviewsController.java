/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.dto.PendingReview;
import com.rainier.me.dto.ReviewStats;
import com.rainier.me.service.MeReviewService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-scoped review queue (v0.0.39). {@code GET /api/me/pending-reviews} returns the stories awaiting
 * review by the caller (identity from {@code SecurityFilter}; token-gated, NOT admin-gated). The
 * read-model behind the「我的评审」/ 架构师评审看板 landing page.
 */
@RestController
@RequestMapping("/api/me")
public class MePendingReviewsController {

  private final MeReviewService service;

  public MePendingReviewsController(MeReviewService service) {
    this.service = service;
  }

  @GetMapping(path = "/pending-reviews", produces = "application/json")
  public List<PendingReview> pendingReviews(HttpServletRequest request) {
    return service.pendingReviews(currentUsername(request));
  }

  /**
   * v0.0.112 (H5): counters for 架构师 landing page — pending Story/Task counts plus this-week
   * APPROVED/REJECTED counts (see {@link ReviewStats}).
   */
  @GetMapping(path = "/review-stats", produces = "application/json")
  public ReviewStats reviewStats(HttpServletRequest request) {
    return service.reviewStats(currentUsername(request));
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
