/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link RoleController} GET/PUT. Covers TC-ROL-004..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RoleRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long createRole(String code, String name) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-ROL-004: GET 详情完整字段集。 */
  @Test
  void get_existingId_returnsFullDetail() throws Exception {
    Long id = createRole("PMO", "PMO");
    MvcResult res =
        mockMvc
            .perform(get("/api/roles/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "name",
      "description",
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

  /** TC-ROL-005: 软删后 GET 404. */
  @Test
  void get_softDeleted_returns404() throws Exception {
    Long id = createRole("PMO", "PMO");
    mockMvc.perform(delete("/api/roles/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/roles/" + id)).andExpect(status().isNotFound());
  }

  /** TC-ROL-006: PUT 更新 name + description. */
  @Test
  void put_updateNameAndDescription_persists() throws Exception {
    Long id = createRole("PMO", "PMO");
    ObjectNode body = json.createObjectNode();
    body.put("name", "Project Management Office");
    body.put("description", "项目管理办公室");
    mockMvc
        .perform(
            put("/api/roles/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Project Management Office"))
        .andExpect(jsonPath("$.description").value("项目管理办公室"));
  }
}
