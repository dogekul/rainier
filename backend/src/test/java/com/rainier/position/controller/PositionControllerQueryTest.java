/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.controller;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.position.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link PositionController} GET / PUT. Covers TC-POS-005..008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PositionControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PositionRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long createPosition(String code, String name, String category) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    body.put("category", category);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/positions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-POS-005: GET 详情完整字段集。 */
  @Test
  void get_existingId_returnsFullDetail() throws Exception {
    Long id = createPosition("BE_ENG", "Backend Engineer", "TECH");
    MvcResult res =
        mockMvc
            .perform(get("/api/positions/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "name",
      "description",
      "category",
      "enabled",
      "createTime",
      "updateTime",
      "createBy",
      "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-POS-006: 按 category 过滤列表。 */
  @Test
  void getList_filterByCategory_returnsOnlyMatching() throws Exception {
    createPosition("BE_ENG", "Backend", "TECH");
    createPosition("FE_ENG", "Frontend", "TECH");
    createPosition("PO", "Product Owner", "PM");
    mockMvc
        .perform(get("/api/positions?category=TECH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].category", contains("TECH", "TECH")));
  }

  /** TC-POS-007: PUT 更新 name + enabled. */
  @Test
  void put_updateNameAndEnabled_persists() throws Exception {
    Long id = createPosition("BE_ENG", "Backend Engineer", "TECH");
    ObjectNode body = json.createObjectNode();
    body.put("name", "Senior Backend Engineer");
    body.put("category", "TECH");
    body.put("enabled", false);
    mockMvc
        .perform(
            put("/api/positions/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Senior Backend Engineer"))
        .andExpect(jsonPath("$.enabled").value(false));
  }

  /** TC-POS-008: PUT body 含 code 静默忽略（DTO 无 code 字段）。 */
  @Test
  void put_withCodeInBody_silentlyIgnored() throws Exception {
    Long id = createPosition("BE_ENG", "Backend Engineer", "TECH");
    ObjectNode body = json.createObjectNode();
    body.put("code", "BE_ENG_V2"); // legacy field — DTO doesn't have it; Jackson ignores
    body.put("name", "Backend Engineer");
    body.put("category", "TECH");
    mockMvc
        .perform(
            put("/api/positions/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("BE_ENG"));
  }
}
