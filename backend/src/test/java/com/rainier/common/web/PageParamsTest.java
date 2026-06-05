/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.diag.PageDiagController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Slice test for {@link PageParams} + {@link PageResponse}.
 *
 * <p>Covers TC-PAG-001 (envelope 字段稳定), TC-PAG-002 (size>100 → 400), TC-PAG-003 (默认值 page=0
 * size=20).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PageDiagController.class)
class PageParamsTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void getPage_noParams_returnsDefaults() throws Exception {
    mockMvc
        .perform(get("/api/_diag/page"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.total").value(0))
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(0));
  }

  @Test
  void getPage_envelopeContainsOnlyFourFields() throws Exception {
    mockMvc
        .perform(get("/api/_diag/page?page=2&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(2))
        .andExpect(jsonPath("$.size").value(10))
        .andExpect(jsonPath("$.total").exists())
        .andExpect(jsonPath("$.content").exists())
        .andExpect(jsonPath("$.pageable").doesNotExist())
        .andExpect(jsonPath("$.totalPages").doesNotExist())
        .andExpect(jsonPath("$.last").doesNotExist())
        .andExpect(jsonPath("$.first").doesNotExist());
  }

  @Test
  void getPage_sizeOver100_returns400FieldError() throws Exception {
    mockMvc
        .perform(get("/api/_diag/page?size=101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("size")));
  }

  @Test
  void getPage_pageBelowZero_returns400FieldError() throws Exception {
    mockMvc
        .perform(get("/api/_diag/page?page=-1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("page")));
  }
}
