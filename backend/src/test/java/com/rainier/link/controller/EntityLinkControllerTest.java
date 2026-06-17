/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.link.repository.EntityLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.31 关联面板 — link CRUD. Covers TC-LINK-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EntityLinkControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityLinkRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private ObjectNode body(String targetType, Long targetId, String linkType, String url) {
    ObjectNode b = json.createObjectNode();
    b.put("targetType", targetType);
    b.put("targetId", targetId);
    b.put("linkType", linkType);
    b.put("url", url);
    return b;
  }

  /** TC-LINK-001: create a STORY link → 201 with fields. */
  @Test
  void post_validStoryLink_returns201() throws Exception {
    ObjectNode b = body("STORY", 5L, "PRD", "https://confluence/x");
    b.put("label", "需求文档");
    mockMvc
        .perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(b.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.targetType").value("STORY"))
        .andExpect(jsonPath("$.targetId").value(5))
        .andExpect(jsonPath("$.linkType").value("PRD"))
        .andExpect(jsonPath("$.label").value("需求文档"))
        .andExpect(jsonPath("$.url").value("https://confluence/x"));
  }

  /** TC-LINK-002: invalid targetType → 400. */
  @Test
  void post_invalidTargetType_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("EPIC", 5L, "PRD", "https://x").toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-LINK-003: invalid linkType → 400. */
  @Test
  void post_invalidLinkType_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("TASK", 5L, "WHAT", "https://x").toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-LINK-004: missing url → 400 (validation). */
  @Test
  void post_missingUrl_returns400() throws Exception {
    ObjectNode b = json.createObjectNode();
    b.put("targetType", "TASK");
    b.put("targetId", 5L);
    b.put("linkType", "PR");
    mockMvc
        .perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(b.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-LINK-005: list by target returns only that target's links, oldest-first. */
  @Test
  void get_listByTarget_returnsScopedLinks() throws Exception {
    mockMvc.perform(
        post("/api/links")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("TASK", 7L, "PR", "https://gitlab/1").toString()));
    mockMvc.perform(
        post("/api/links")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("TASK", 7L, "DEFECT", "https://zentao/2").toString()));
    mockMvc.perform(
        post("/api/links")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("TASK", 99L, "PR", "https://gitlab/other").toString()));

    mockMvc
        .perform(get("/api/links").param("targetType", "TASK").param("targetId", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[0].url").value("https://gitlab/1"));
  }

  /** TC-LINK-006: delete removes the link from the list. */
  @Test
  void delete_removesLink() throws Exception {
    String resp =
        mockMvc
            .perform(
                post("/api/links")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body("STORY", 3L, "DESIGN", "https://figma/x").toString()))
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long id = json.readTree(resp).get("id").asLong();

    mockMvc.perform(delete("/api/links/" + id)).andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/links").param("targetType", "STORY").param("targetId", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
