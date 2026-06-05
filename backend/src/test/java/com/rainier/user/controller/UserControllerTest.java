/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link UserController}. Covers TC-USR-001..011. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  @Test
  void post_minimalPayload_returns201WithDefaults() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "alice");
    body.put("name", "Alice");

    mockMvc
        .perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/users/[0-9a-f]{32}")))
        .andExpect(jsonPath("$.id", matchesPattern("[0-9a-f]{32}")))
        .andExpect(jsonPath("$.loginName").value("alice"))
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.code").isEmpty())
        .andExpect(jsonPath("$.emailAddress").isEmpty())
        .andExpect(jsonPath("$.isInternal").value(true))
        .andExpect(jsonPath("$.enabled").value(true))
        .andExpect(jsonPath("$.delFlag").value(false));
  }

  @Test
  void post_duplicateLoginName_returns409() throws Exception {
    create("alice", "Alice", null, null);
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "alice");
    body.put("name", "Other Alice");
    mockMvc
        .perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("loginName already exists")));
  }

  @Test
  void post_duplicateCode_returns409() throws Exception {
    create("alice", "Alice", "E1001", null);
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "bob");
    body.put("name", "Bob");
    body.put("code", "E1001");
    mockMvc
        .perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists")));
  }

  @Test
  void post_invalidEmail_returns400FieldError() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "alice");
    body.put("name", "Alice");
    body.put("emailAddress", "not-an-email");
    mockMvc
        .perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("emailAddress")));
  }

  @Test
  void get_existingId_returns200() throws Exception {
    String id = create("alice", "Alice", null, null);
    mockMvc
        .perform(get("/api/users/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginName").value("alice"));
  }

  @Test
  void get_softDeletedId_returns404() throws Exception {
    String id = create("alice", "Alice", null, null);
    mockMvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/users/" + id)).andExpect(status().isNotFound());
  }

  @Test
  void getList_searchMatchesAcrossFields() throws Exception {
    create("alice", "Alice Wong", "E1001", "alice@x.com");
    create("bob", "Bob", "E2001", "bob@x.com");
    mockMvc
        .perform(get("/api/users?search=alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].loginName").value("alice"));
  }

  @Test
  void getList_filterByIsInternalFalse() throws Exception {
    createWithFlags("alice", "Alice", true);
    createWithFlags("bob", "Bob", false);
    createWithFlags("carol", "Carol", false);
    mockMvc
        .perform(get("/api/users?isInternal=false"))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].isInternal", hasItem(false)));
  }

  @Test
  void put_modifyNameAndEnabled_loginNameUnchanged() throws Exception {
    String id = create("alice", "Alice", null, null);
    ObjectNode body = json.createObjectNode();
    body.put("name", "Alice Wang");
    body.put("enabled", false);
    mockMvc
        .perform(
            put("/api/users/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Alice Wang"))
        .andExpect(jsonPath("$.enabled").value(false))
        .andExpect(jsonPath("$.loginName").value("alice"));
  }

  @Test
  void delete_noOrgAssignments_returns204() throws Exception {
    String id = create("alice", "Alice", null, null);
    mockMvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/users/" + id)).andExpect(status().isNotFound());
  }

  // ---------------------------- helpers ----------------------------------

  private String create(String loginName, String name, String code, String email) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", loginName);
    body.put("name", name);
    if (code != null) {
      body.put("code", code);
    }
    if (email != null) {
      body.put("emailAddress", email);
    }
    MvcResult r =
        mockMvc
            .perform(
                post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
  }

  private String createWithFlags(String loginName, String name, boolean isInternal)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", loginName);
    body.put("name", name);
    body.put("isInternal", isInternal);
    MvcResult r =
        mockMvc
            .perform(
                post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
  }
}
