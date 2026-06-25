/* (C) 2026 Rainier — internal use only. */
package com.rainier.password.controller;

import com.rainier.password.dto.AdminResetPasswordRequest;
import com.rainier.password.service.PasswordService;
import java.util.Collections;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.76 B3 — admin override: reset a target user's password without knowing the current one.
 *
 * <p>Path lives under {@code /api/admin/**} so {@link com.rainier.authz.AdminPaths} (Tier A) gates
 * it. Tests with admin-authz disabled (the default test profile) hit it unauthenticated.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserPasswordController {

  private final PasswordService service;

  public AdminUserPasswordController(PasswordService service) {
    this.service = service;
  }

  @PostMapping(
      path = "/{id}/reset-password",
      consumes = "application/json",
      produces = "application/json")
  public Map<String, Boolean> reset(
      @PathVariable Long id, @RequestBody(required = false) AdminResetPasswordRequest req) {
    String next = req == null ? null : req.getNewPassword();
    service.adminResetPassword(id, next);
    return Collections.singletonMap("ok", Boolean.TRUE);
  }
}
