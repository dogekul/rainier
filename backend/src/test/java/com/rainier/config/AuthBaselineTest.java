/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.27 — identity baseline. Flips BOTH flags on (require-all-users-token + admin-authz) to verify
 * the gate AND that 401 (identity, SecurityFilter) precedes 403 (authz, AdminAuthorizationInterceptor).
 * Covers TC-AUTHB-001..007.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "app.security.require-all-users-token.enabled=true",
      "app.security.admin-authz.enabled=true"
    })
class AuthBaselineTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private ObjectMapper json;

  /** TC-AUTHB-001: all-users endpoint without a token → 401. */
  @Test
  void getProjects_noToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/projects"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Missing or invalid token"));
  }

  /** TC-AUTHB-002: all-users endpoint WITH a valid token → not 401 (passes identity). */
  @Test
  void getProjects_validToken_passes() throws Exception {
    String token = authService.issueToken("alice");
    mockMvc
        .perform(get("/api/projects").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  /** TC-AUTHB-003: invalid token → 401. */
  @Test
  void getTasks_invalidToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/tasks").header("Authorization", "Bearer not-a-real-token"))
        .andExpect(status().isUnauthorized());
  }

  /** TC-AUTHB-004: GET /api/health is whitelisted → 200 without a token. */
  @Test
  void getHealth_noToken_returns200() throws Exception {
    mockMvc.perform(get("/api/health")).andExpect(status().isOk());
  }

  /** TC-AUTHB-005: POST /api/auth/login is whitelisted → not 401 without a token. */
  @Test
  void postLogin_noToken_notGated() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("username", "alice");
    body.put("password", "x");
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
  }

  /** TC-AUTHB-006: an admin endpoint without a token → 401 (identity), NOT 403 (authz). */
  @Test
  void getRoles_noToken_returns401_beforeAuthz() throws Exception {
    mockMvc.perform(get("/api/roles")).andExpect(status().isUnauthorized());
  }

  /** TC-AUTHB-007: an admin endpoint with a valid non-admin token → 403 (identity passed, authz denied). */
  @Test
  void getRoles_nonAdminToken_returns403() throws Exception {
    String token = authService.issueToken("ghost"); // no User → not elevated
    mockMvc
        .perform(get("/api/roles").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
