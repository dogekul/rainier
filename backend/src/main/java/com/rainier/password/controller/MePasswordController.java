/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.exception.UnauthorizedException;
import com.rainier.password.dto.ChangePasswordRequest;
import com.rainier.password.service.PasswordService;
import java.util.Collections;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** v0.0.76 B3 — self-service password change. */
@RestController
@RequestMapping("/api/me")
public class MePasswordController {

  private final PasswordService service;

  public MePasswordController(PasswordService service) {
    this.service = service;
  }

  @PostMapping(path = "/password", consumes = "application/json", produces = "application/json")
  public Map<String, Boolean> changeOwnPassword(
      HttpServletRequest request, @RequestBody(required = false) ChangePasswordRequest req) {
    String username = currentUsername(request);
    String current = req == null ? null : req.getCurrentPassword();
    String next = req == null ? null : req.getNewPassword();
    service.changeOwnPassword(username, current, next);
    return Collections.singletonMap("ok", Boolean.TRUE);
  }

  private static String currentUsername(HttpServletRequest request) {
    Object username = request.getAttribute(AuthController.ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return (String) username;
  }
}
