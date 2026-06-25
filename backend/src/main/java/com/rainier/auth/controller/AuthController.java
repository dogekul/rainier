/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import com.rainier.auth.dto.LoginRequest;
import com.rainier.auth.dto.LoginResponse;
import com.rainier.auth.dto.MeResponse;
import com.rainier.auth.dto.UserDto;
import com.rainier.auth.idp.IdentityProvider;
import com.rainier.auth.idp.UserIdentity;
import com.rainier.auth.service.AuthService;
import com.rainier.auth.service.MeService;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.UnauthorizedException;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints: login (issue mock JWT) and current-user lookup.
 *
 * <p>Covers spec {@code auth-placeholder} for both Requirements. v0.0.75 (B2): credential
 * verification is delegated to the {@link IdentityProvider} chain (LocalDb + optional LDAP/OAuth
 * stubs); the first non-empty result wins.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

  /** Request attribute set by {@code SecurityFilter} after verifying a Bearer token. */
  public static final String ATTR_USERNAME = "rainier.username";

  private final AuthService authService;
  private final MeService meService;
  private final List<IdentityProvider> identityProviders;
  private final boolean realAuthEnabled;

  public AuthController(
      AuthService authService,
      MeService meService,
      List<IdentityProvider> identityProviders,
      @Value("${app.security.real-auth.enabled:false}") boolean realAuthEnabled) {
    this.authService = authService;
    this.meService = meService;
    this.identityProviders = identityProviders;
    this.realAuthEnabled = realAuthEnabled;
  }

  @PostMapping(path = "/login", produces = "application/json", consumes = "application/json")
  public LoginResponse login(@RequestBody(required = false) LoginRequest req) {
    if (req == null || isBlank(req.getUsername())) {
      throw new BadRequestException("username is required");
    }
    if (isBlank(req.getPassword())) {
      throw new BadRequestException("password is required");
    }
    // v0.0.38 / v0.0.75: real verification flows through the IdentityProvider chain. Flag off →
    // mock issuer (any creds, dev only). Unknown user OR wrong password OR no provider accepts →
    // single 401 (same message either way, to avoid leaking which login names exist).
    String resolvedLoginName = req.getUsername();
    if (realAuthEnabled) {
      Optional<UserIdentity> accepted = Optional.empty();
      for (IdentityProvider provider : identityProviders) {
        Optional<UserIdentity> result = provider.authenticate(req.getUsername(), req.getPassword());
        if (result.isPresent()) {
          accepted = result;
          break;
        }
      }
      if (!accepted.isPresent()) {
        throw new UnauthorizedException("invalid username or password");
      }
      // Use the canonical loginName the provider resolved (e.g. LDAP may canonicalize case).
      String providerLogin = accepted.get().getLoginName();
      if (providerLogin != null && !providerLogin.isEmpty()) {
        resolvedLoginName = providerLogin;
      }
    }
    String token = authService.issueToken(resolvedLoginName);
    return new LoginResponse(token, new UserDto(resolvedLoginName));
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
