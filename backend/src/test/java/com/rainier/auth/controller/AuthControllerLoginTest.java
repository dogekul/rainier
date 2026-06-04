/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainier.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Covers TC-AUT-001 (login success) and TC-AUT-002 (missing field rejected).
 *
 * <p>Verifies the JWT is HS256, three-segment, has {@code sub == username}, and {@code exp ≈
 * now+24h} by re-parsing with the same signing key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerLoginTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthService authService;

  @Value("${app.security.jwt.secret}")
  private String secret;

  @Value("${app.security.jwt.expiration-hours}")
  private long expirationHours;

  @Test
  void login_withValidCredentials_returnsJwtAndUser() throws Exception {
    long beforeMillis = System.currentTimeMillis();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"alice\",\"password\":\"any\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("alice"))
            .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    String token = body.get("token").asText();

    // JWT three-segment shape
    assertThat(token.split("\\.")).hasSize(3);

    // Verify sub + exp by re-parsing with the same key
    SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();

    assertThat(claims.getSubject()).isEqualTo("alice");
    long expectedExpMillis = beforeMillis + expirationHours * 3600L * 1000L;
    long actualExpMillis = claims.getExpiration().getTime();
    // Tolerate up to 60s of clock drift between beforeMillis and the JWT exp.
    assertThat(actualExpMillis).isBetween(expectedExpMillis - 1000L, expectedExpMillis + 60_000L);
    assertThat(claims.getExpiration()).isAfter(new Date(beforeMillis));

    // Cross-check: AuthService also parses it back to alice.
    assertThat(authService.parseUsername(token)).isEqualTo("alice");
  }

  @Test
  void login_withMissingUsername_returns400Json() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void login_withEmptyUsername_returns400Json() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"\",\"password\":\"x\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").isNotEmpty());
  }
}
