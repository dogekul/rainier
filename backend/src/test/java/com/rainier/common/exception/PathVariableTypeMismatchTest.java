/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies the v1→v2 id migration handler (TC-MIG-002): when a {@code @PathVariable Long id}
 * receives a non-numeric value, the response is a structured 400 JSON, not a 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PathVariableTypeMismatchTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void getOrganization_nonNumericId_returns400Json() throws Exception {
    mockMvc
        .perform(get("/api/organizations/not-a-number"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(content().string(not(containsString("Exception"))));
  }
}
