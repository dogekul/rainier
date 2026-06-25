/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.69 (A5): {@code POST /api/me/ai-auth-level} persists and {@code GET /api/auth/me} echoes it.
 * Seeds its own test user so it doesn't depend on the demo dataset (test profile has no seed).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeAiAuthLevelControllerTest {

  private static final String LOGIN = "a5testuser";

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void seedUser() {
    if (!userRepo.findByLoginName(LOGIN).isPresent()) {
      User u = new User();
      u.setLoginName(LOGIN);
      u.setName("A5 Test User");
      u.setIsInternal(Boolean.TRUE);
      u.setEnabled(Boolean.TRUE);
      userRepo.saveAndFlush(u);
    } else {
      // Reset to null between methods so the default-BASIC assertion holds.
      User u = userRepo.findByLoginName(LOGIN).get();
      u.setAiAuthLevel(null);
      userRepo.saveAndFlush(u);
    }
  }

  @Test
  void setLevel_persists_andMeEchoes() throws Exception {
    String token = authService.issueToken(LOGIN);

    // Default (no setting yet) — getter coalesces null → BASIC.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiAuthLevel").value("BASIC"));

    // Set DEPTH.
    mockMvc
        .perform(
            post("/api/me/ai-auth-level")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"DEPTH\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiAuthLevel").value("DEPTH"));

    // Verify persistence via /api/auth/me.
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.aiAuthLevel").value("DEPTH"));

    // Reset to BASIC so the run doesn't leak state into sibling tests.
    mockMvc
        .perform(
            post("/api/me/ai-auth-level")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"BASIC\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void setLevel_invalidEnum_returns400() throws Exception {
    String token = authService.issueToken(LOGIN);
    mockMvc
        .perform(
            post("/api/me/ai-auth-level")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"GOD\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void setLevel_missingLevel_returns400() throws Exception {
    String token = authService.issueToken(LOGIN);
    mockMvc
        .perform(
            post("/api/me/ai-auth-level")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void setLevel_missingToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/me/ai-auth-level")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"level\":\"DEPTH\"}"))
        .andExpect(status().isUnauthorized());
  }
}
