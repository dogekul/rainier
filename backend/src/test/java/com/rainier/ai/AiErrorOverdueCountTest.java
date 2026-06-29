/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.ai.domain.AiError;
import com.rainier.ai.domain.AiErrorStatus;
import com.rainier.ai.repository.AiErrorRepository;
import com.rainier.ai.service.AiErrorService;
import com.rainier.common.exception.BadRequestException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * F5 (v0.0.104) — AiErrorService.countOverdueOpen + GET /api/ai/errors/overdue-count. Seeds rows
 * with explicit occurredAt timestamps and asserts only OPEN-and-older-than-threshold are counted.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AiErrorOverdueCountTest {

  @Autowired private AiErrorService service;
  @Autowired private AiErrorRepository repo;
  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private void seed(String stat, LocalDateTime occurredAt) {
    AiError e = new AiError();
    e.setOccurredAt(occurredAt);
    e.setAiAction("ACT");
    e.setErrorDesc("desc");
    e.setStatus(stat);
    repo.saveAndFlush(e);
  }

  @Test
  void countOverdueOpen_countsOnlyOpenAndOlderThanThreshold() {
    LocalDateTime now = LocalDateTime.now();
    // 2 OPEN overdue (older than 24h)
    seed(AiErrorStatus.OPEN, now.minusHours(30));
    seed(AiErrorStatus.OPEN, now.minusHours(48));
    // 2 OPEN fresh (within 24h) — must not count
    seed(AiErrorStatus.OPEN, now.minusHours(2));
    seed(AiErrorStatus.OPEN, now.minusMinutes(30));
    // 1 FIXED very old — must not count even though old
    seed(AiErrorStatus.FIXED, now.minusHours(100));

    assertThat(service.countOverdueOpen(24)).isEqualTo(2L);
  }

  @Test
  void countOverdueOpen_zeroOrNegativeHours_throws400() {
    assertThatThrownBy(() -> service.countOverdueOpen(0))
        .isInstanceOf(BadRequestException.class);
    assertThatThrownBy(() -> service.countOverdueOpen(-1))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void getOverdueCount_defaultsTo24h_andReturnsCountAndThreshold() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    seed(AiErrorStatus.OPEN, now.minusHours(25));
    seed(AiErrorStatus.OPEN, now.minusHours(1));

    mockMvc
        .perform(get("/api/ai/errors/overdue-count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.thresholdHours").value(24));
  }

  @Test
  void getOverdueCount_explicitHours_paramHonored() throws Exception {
    LocalDateTime now = LocalDateTime.now();
    seed(AiErrorStatus.OPEN, now.minusHours(3));
    seed(AiErrorStatus.OPEN, now.minusMinutes(30));

    mockMvc
        .perform(get("/api/ai/errors/overdue-count").param("hours", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1))
        .andExpect(jsonPath("$.thresholdHours").value(2));
  }
}
