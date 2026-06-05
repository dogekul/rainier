/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@code POST /api/organizations}.
 *
 * <p>Covers TC-ORG-001 (root create), 002 (child create + path/whole_name derivation), 003 (missing
 * name → 400), 004 (parent+code collision → 409), 005 (parent not found → 400).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  @Test
  void post_validRootPayload_returns201LocationAndDerivedPath() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", "COMPANY");
    body.put("code", "HQ");
    body.put("name", "总公司");
    body.put("description", "总部");

    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/organizations/[0-9a-f]{32}")))
        .andExpect(jsonPath("$.id", matchesPattern("[0-9a-f]{32}")))
        .andExpect(jsonPath("$.parentId").isEmpty())
        .andExpect(jsonPath("$.type").value("COMPANY"))
        .andExpect(jsonPath("$.code").value("HQ"))
        .andExpect(jsonPath("$.name").value("总公司"))
        .andExpect(jsonPath("$.path", matchesPattern("/[0-9a-f]{32}")))
        .andExpect(jsonPath("$.wholeName").value("总公司"))
        .andExpect(jsonPath("$.isPmo").value(false))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.createTime").exists())
        .andExpect(jsonPath("$.updateTime").exists());
  }

  @Test
  void post_validChildPayload_derivesPathAndWholeNameFromParent() throws Exception {
    String parentId = createRoot();

    ObjectNode body = json.createObjectNode();
    body.put("parentId", parentId);
    body.put("type", "DEPARTMENT");
    body.put("code", "RD");
    body.put("name", "研发部");

    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parentId").value(parentId))
        .andExpect(jsonPath("$.path", startsWith("/" + parentId + "/")))
        .andExpect(jsonPath("$.wholeName").value("总公司/研发部"));
  }

  @Test
  void post_missingName_returns400FieldError() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", "COMPANY");
    body.put("code", "X");

    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Validation failed"))
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")));
  }

  @Test
  void post_duplicateCodeUnderSameParent_returns409() throws Exception {
    String parentId = createRoot();
    ObjectNode body = json.createObjectNode();
    body.put("parentId", parentId);
    body.put("type", "DEPARTMENT");
    body.put("code", "RD");
    body.put("name", "研发部");
    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    // second create — same parent + same code
    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  @Test
  void post_parentNotFound_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("parentId", "ghost00000000000000000000000000");
    body.put("type", "DEPARTMENT");
    body.put("code", "X");
    body.put("name", "X");
    mockMvc
        .perform(
            post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("parent organization not found")));
  }

  private String createRoot() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", "COMPANY");
    body.put("code", "HQ");
    body.put("name", "总公司");
    MvcResult result =
        mockMvc
            .perform(
                post("/api/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }
}
