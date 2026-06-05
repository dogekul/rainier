/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.diag.BoomController;
import com.rainier.diag.ValidationDiagController;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link GlobalExceptionHandler}.
 *
 * <p>Covers TC-BES-002 (未知 endpoint → JSON 404), TC-BES-003 (RuntimeException → JSON 500 without
 * stack trace), and TC-BES-202 (Bean Validation → 400 含 fieldErrors[]). Also exercises the
 * NotFoundException → 404 and ConflictException → 409 paths added by change
 * 2026-06-04-org-tree-and-employee.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({BoomController.class, ValidationDiagController.class})
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void getUnknownEndpoint_returns404Json() throws Exception {
    mockMvc
        .perform(get("/api/this-does-not-exist"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void getBoomEndpoint_returns500JsonWithoutStackTrace() throws Exception {
    mockMvc
        .perform(get("/api/_diag/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(content().string(not(containsString("at com.rainier"))))
        .andExpect(content().string(not(containsString("Exception"))));
  }

  @Test
  void getNotFoundException_returns404JsonWithMessage() throws Exception {
    mockMvc
        .perform(get("/api/_diag/not-found"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("widget 42 does not exist"));
  }

  @Test
  void getConflictException_returns409JsonWithMessage() throws Exception {
    mockMvc
        .perform(get("/api/_diag/conflict"))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("widget 42 already exists"));
  }

  @Test
  void postInvalidPayload_returns400JsonWithFieldErrors() throws Exception {
    // missing "name" (required) and "nickname" too short
    String body = "{\"nickname\":\"a\"}";
    mockMvc
        .perform(post("/api/_diag/echo").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.fieldErrors").isArray())
        .andExpect(jsonPath("$.fieldErrors[*].field", Matchers.hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", Matchers.hasItem("nickname")));
  }
}
