/* (C) 2026 Rainier — internal use only. */
package com.rainier.storage.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration test for {@link FilesController}: upload via multipart then download via the
 * returned accessUrl, and verify byte-for-byte equality + 404 on missing key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FilesControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void uploadThenDownload_returnsSameBytes() throws Exception {
    byte[] payload = "bye".getBytes();
    MockMultipartFile file =
        new MockMultipartFile("file", "hi.txt", "text/plain", payload);

    MvcResult uploaded =
        mockMvc
            .perform(multipart("/api/files").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.storageType").value("LOCAL"))
            .andExpect(jsonPath("$.storedKey").isNotEmpty())
            .andExpect(jsonPath("$.accessUrl").isNotEmpty())
            .andReturn();

    JsonNode body = objectMapper.readTree(uploaded.getResponse().getContentAsString());
    String accessUrl = body.get("accessUrl").asText();
    assertThat(accessUrl).startsWith("/api/files/");

    MvcResult downloaded =
        mockMvc.perform(get(accessUrl)).andExpect(status().isOk()).andReturn();
    assertThat(downloaded.getResponse().getContentAsByteArray()).isEqualTo(payload);
    assertThat(downloaded.getResponse().getContentType()).contains("text/plain");
  }

  @Test
  void downloadMissingKey_returns404() throws Exception {
    mockMvc.perform(get("/api/files/202606/does-not-exist-xyz.bin")).andExpect(status().isNotFound());
  }

  @Test
  void uploadEmptyFile_returns400() throws Exception {
    MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
    mockMvc.perform(multipart("/api/files").file(empty)).andExpect(status().isBadRequest());
  }
}
