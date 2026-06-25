/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * v0.0.74 (B1) — end-to-end regression for the {@code require-all-users-token} baseline gate, run
 * through the real servlet stack ({@link SpringBootTest.WebEnvironment#RANDOM_PORT}) rather than
 * MockMvc. Flips the flag on via {@link TestPropertySource}; covers the four B1 scenarios:
 *
 * <ol>
 *   <li>no token + protected endpoint ({@code /api/me/inbox}) → 401
 *   <li>no token + whitelisted endpoint ({@code POST /api/auth/login}) → NOT a SecurityFilter 401
 *   <li>valid token + protected endpoint → 200
 *   <li>matrix-parameter bypass ({@code /api/me/inbox;x=1}) → 401 (UrlPathHelper strip)
 * </ol>
 *
 * <p>Complements the MockMvc-based {@code AuthBaselineTest} (v0.0.27): same gate, real HTTP path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.security.require-all-users-token.enabled=true"})
class GlobalAuthBaselineTest {

  @Autowired private TestRestTemplate rest;
  @Autowired private AuthService authService;
  @Autowired private ObjectMapper json;

  /** No token on a protected endpoint → 401 with the canonical SecurityFilter message. */
  @Test
  void inbox_noToken_returns401() throws Exception {
    ResponseEntity<String> resp = rest.getForEntity("/api/me/inbox", String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode body = json.readTree(resp.getBody());
    assertThat(body.path("message").asText()).contains("Missing or invalid token");
  }

  /** Valid token on a protected endpoint → 200 (inbox degrades to empty for unknown subjects). */
  @Test
  void inbox_validToken_returns200() {
    String token = authService.issueToken("alice");
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);

    ResponseEntity<String> resp =
        rest.exchange("/api/me/inbox", HttpMethod.GET, new HttpEntity<>(headers), String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /**
   * Whitelisted {@code POST /api/auth/login} is NOT blocked by SecurityFilter — anonymous request
   * reaches the controller. Whether the credentials succeed or fail is irrelevant; only assert it
   * is not the SecurityFilter 401 (no "Missing or invalid token" body).
   */
  @Test
  void login_noToken_notBlockedBySecurityFilter() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("username", "alice");
    body.put("password", "rainier123");
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    ResponseEntity<String> resp =
        rest.exchange(
            "/api/auth/login",
            HttpMethod.POST,
            new HttpEntity<>(body.toString(), headers),
            String.class);

    // Either 200 (test profile real-auth=false → any password) or a non-401 4xx — but never the
    // SecurityFilter "Missing or invalid token" 401.
    if (resp.getStatusCode() == HttpStatus.UNAUTHORIZED) {
      JsonNode parsed = json.readTree(resp.getBody());
      assertThat(parsed.path("message").asText()).doesNotContain("Missing or invalid token");
    } else {
      assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
    }
  }

  /** Matrix-parameter bypass: {@code /api/me/inbox;x=1} MUST also be gated to 401. */
  @Test
  void inbox_matrixParam_noToken_returns401() throws Exception {
    ResponseEntity<String> resp = rest.getForEntity("/api/me/inbox;x=1", String.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    JsonNode body = json.readTree(resp.getBody());
    assertThat(body.path("message").asText()).contains("Missing or invalid token");
  }
}
