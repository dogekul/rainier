/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.controller;

import com.rainier.password.dto.ForgotPasswordRequest;
import com.rainier.password.dto.ResetPasswordRequest;
import com.rainier.password.service.PasswordService;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.76 B3 — anonymous forgot/reset flow. Both endpoints whitelisted in {@link
 * com.rainier.config.SecurityWhitelistPaths}.
 */
@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

  private final PasswordService service;

  public PasswordResetController(PasswordService service) {
    this.service = service;
  }

  @PostMapping(
      path = "/forgot-password",
      consumes = "application/json",
      produces = "application/json")
  public Map<String, Boolean> forgotPassword(
      @RequestBody(required = false) ForgotPasswordRequest req) {
    String loginName = req == null ? null : req.getLoginName();
    String email = req == null ? null : req.getEmail();
    service.issueResetToken(loginName, email);
    return Collections.singletonMap("ok", Boolean.TRUE);
  }

  @PostMapping(
      path = "/reset-password",
      consumes = "application/json",
      produces = "application/json")
  public Map<String, Boolean> resetPassword(
      @RequestBody(required = false) ResetPasswordRequest req) {
    String token = req == null ? null : req.getToken();
    String newPassword = req == null ? null : req.getNewPassword();
    service.redeemResetToken(token, newPassword);
    return Collections.singletonMap("ok", Boolean.TRUE);
  }
}
