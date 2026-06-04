/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Covers TC-AUT-003 (valid token → 200) and TC-AUT-004 (missing / malformed / expired / bad
 * signature → 401 JSON without stack trace).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerMeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;

  @Test
  void getMe_withValidToken_returnsUsername() throws Exception {
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.username").value("alice"));
  }

  @Test
  void getMe_withoutAuthorizationHeader_returns401Json() throws Exception {
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(content().string(not(containsString("at com.rainier"))));
  }

  @Test
  void getMe_withMalformedToken_returns401Json() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer not.a.jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void getMe_withWrongSignature_returns401Json() throws Exception {
    SecretKey foreignKey =
        Keys.hmacShaKeyFor(
            "another-256-bit-secret-not-the-rainier-one-padding-padding-padding"
                .getBytes(StandardCharsets.UTF_8));
    Date now = new Date();
    String foreignToken =
        Jwts.builder()
            .setSubject("alice")
            .setIssuedAt(now)
            .setExpiration(new Date(now.getTime() + 60_000L))
            .signWith(foreignKey, SignatureAlgorithm.HS256)
            .compact();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + foreignToken))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void getMe_withExpiredToken_returns401Json() throws Exception {
    // Build an already-expired token using the SAME signing key as production by re-reading the
    // service's signing secret via reflection-free path: issue a normal token then craft an
    // expired one using its key. Simpler: use the service's secret via @Value in a dedicated test.
    // For brevity we rely on the same secret used by AuthService (configured in application.yml).
    SecretKey key =
        Keys.hmacShaKeyFor(
            "rainier-dev-only-jwt-secret-must-be-at-least-256-bits-long-changeme-changeme"
                .getBytes(StandardCharsets.UTF_8));
    Date past = new Date(System.currentTimeMillis() - 120_000L);
    String expired =
        Jwts.builder()
            .setSubject("alice")
            .setIssuedAt(new Date(past.getTime() - 60_000L))
            .setExpiration(past)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}
