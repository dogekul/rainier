/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.me.dto.InboxResponse;
import com.rainier.me.service.MeInboxService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PO 需求收件箱 (v0.0.42). {@code GET /api/me/inbox} returns unconverted demands (triage queue) + the
 * caller's own requirements (identity from {@code SecurityFilter}; token-gated, NOT admin-gated). The
 * read-model behind the「需求收件箱」landing page.
 */
@RestController
@RequestMapping("/api/me")
public class MeInboxController {

  private final MeInboxService service;

  public MeInboxController(MeInboxService service) {
    this.service = service;
  }

  @GetMapping(path = "/inbox", produces = "application/json")
  public InboxResponse inbox(
      HttpServletRequest request,
      @RequestParam(name = "productId", required = false) Long productId) {
    return service.inbox(currentUsername(request), productId);
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
