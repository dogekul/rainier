/* (C) 2026 Rainier — internal use only. */
package com.rainier.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests CORS preflight against an existing handler ({@code /api/health}) to verify that {@link
 * CorsConfig} grants the configured origins.
 *
 * <p>Covers TC-BES-004. Spec scenario points to {@code /api/auth/login} but Spring only emits CORS
 * preflight responses for paths that have at least one registered handler; we therefore exercise
 * the preflight on {@code /api/health} (which exists from SLICE-B02). The CORS policy is global
 * across {@code /api/**}, so any path under the prefix exercises the same configuration.
 * (Documented in {@code pending-adjustments.md}.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CorsConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void preflight_fromAllowedOrigin_includesAllowHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/health")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
        .andExpect(header().string("Access-Control-Allow-Methods", Matchers.containsString("POST")))
        .andExpect(
            header()
                .string("Access-Control-Allow-Headers", Matchers.containsString("Authorization")));
  }
}
