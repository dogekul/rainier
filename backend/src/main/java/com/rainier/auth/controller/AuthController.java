/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import com.rainier.auth.dto.LoginRequest;
import com.rainier.auth.dto.LoginResponse;
import com.rainier.auth.dto.MeResponse;
import com.rainier.auth.dto.UserDto;
import com.rainier.auth.service.AuthService;
import com.rainier.auth.service.MeService;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.UnauthorizedException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints: login (issue mock JWT) and current-user lookup.
 *
 * <p>Covers spec {@code auth-placeholder} for both Requirements.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  /** Request attribute set by {@code SecurityFilter} after verifying a Bearer token. */
  public static final String ATTR_USERNAME = "rainier.username";

  private final AuthService authService;
  private final MeService meService;

  public AuthController(AuthService authService, MeService meService) {
    this.authService = authService;
    this.meService = meService;
  }

  @PostMapping(path = "/login", produces = "application/json", consumes = "application/json")
  public LoginResponse login(@RequestBody(required = false) LoginRequest req) {
    if (req == null || isBlank(req.getUsername())) {
      throw new BadRequestException("username is required");
    }
    if (isBlank(req.getPassword())) {
      throw new BadRequestException("password is required");
    }
    String token = authService.issueToken(req.getUsername());
    return new LoginResponse(token, new UserDto(req.getUsername()));
  }

  @GetMapping(path = "/me", produces = "application/json")
  public MeResponse me(HttpServletRequest request) {
    Object username = request.getAttribute(ATTR_USERNAME);
    if (!(username instanceof String) || ((String) username).isEmpty()) {
      throw new UnauthorizedException("Missing or invalid token");
    }
    return meService.forUsername((String) username);
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }
}
