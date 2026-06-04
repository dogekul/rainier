/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.service;

import com.rainier.common.exception.UnauthorizedException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Mock JWT issuer + parser.
 *
 * <p>v0 placeholder: signs HS256 with a hardcoded dev secret from configuration. Will be replaced
 * by a real identity-provider integration in a future STDD change. See {@code design.md §4}.
 */
@Service
public class AuthService {

  private final SecretKey key;
  private final long expirationMillis;

  public AuthService(
      @Value("${app.security.jwt.secret}") String secret,
      @Value("${app.security.jwt.expiration-hours}") long expirationHours) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMillis = expirationHours * 3600L * 1000L;
  }

  /** Returns a signed HS256 JWT whose {@code sub} claim equals {@code username}. */
  public String issueToken(String username) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMillis);
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  /**
   * Parses {@code token} and returns the {@code sub} claim.
   *
   * @throws UnauthorizedException if the token is missing, malformed, expired, or has an invalid
   *     signature.
   */
  public String parseUsername(String token) {
    if (token == null || token.isEmpty()) {
      throw new UnauthorizedException("Missing token");
    }
    try {
      return Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getBody()
          .getSubject();
    } catch (JwtException ex) {
      throw new UnauthorizedException("Invalid or expired token");
    }
  }
}
