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

  @Autowired private com.rainier.position.repository.PositionRepository userTestPositionRepo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    // v0.0.7 hygiene: TC-USR-001/003/004 create Position rows; clean them too so codes
    // BE_ENG / PO don't accumulate across tests within the same run (service-only
    // uniqueness — no DB UNIQUE on code).
    userTestPositionRepo.deleteAll();
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
        .andExpect(header().string("Location", matchesPattern("/api/users/\\d+")))
        .andExpect(jsonPath("$.id").isNumber())
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
    Long id = create("alice", "Alice", null, null);
    mockMvc
        .perform(get("/api/users/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginName").value("alice"));
  }

  @Test
  void get_softDeletedId_returns404() throws Exception {
    Long id = create("alice", "Alice", null, null);
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
    Long id = create("alice", "Alice", null, null);
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
    Long id = create("alice", "Alice", null, null);
    mockMvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/users/" + id)).andExpect(status().isNotFound());
  }

  // ---------------------------- helpers ----------------------------------

  private Long create(String loginName, String name, String code, String email) throws Exception {
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
    return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
  }

  private Long createWithFlags(String loginName, String name, boolean isInternal) throws Exception {
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
    return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
  }

  // ===================== v0.0.7 Position MODIFIED block (TC-USR-001..004) =====================

  @org.springframework.beans.factory.annotation.Autowired
  private com.rainier.position.repository.PositionRepository positionRepo;

  private Long createPosition(String code, String name, String category) {
    com.rainier.position.domain.Position p = new com.rainier.position.domain.Position();
    p.setCode(code);
    p.setName(name);
    p.setCategory(category);
    p.setEnabled(true);
    return positionRepo.saveAndFlush(p).getId();
  }

  /** TC-USR-001: POST 含 positionId 创建 + 富化. */
  @Test
  void post_withPositionId_returnsEnrichedDetail() throws Exception {
    Long positionId = createPosition("BE_ENG", "Backend Engineer", "TECH");
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "alice");
    body.put("name", "Alice");
    body.put("positionId", positionId);
    mockMvc
        .perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.positionId").value(positionId))
        .andExpect(jsonPath("$.positionName").value("Backend Engineer"))
        .andExpect(jsonPath("$.positionCategory").value("TECH"));
  }

  /** TC-USR-002: POST positionId 不存在 → 400. */
  @Test
  void post_unknownPositionId_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", "bob");
    body.put("name", "Bob");
    body.put("positionId", 999_999L);
    mockMvc
        .perform(
            post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("position not found")));
  }

  /** TC-USR-003: PUT 更新 positionId 后富化跟随. */
  @Test
  void put_updatePositionId_enrichmentFollows() throws Exception {
    Long pos1 = createPosition("BE_ENG", "Backend Engineer", "TECH");
    Long pos2 = createPosition("PO", "Product Owner", "PM");
    Long userId = createWithFlags("alice", "Alice", true);
    // first set positionId to pos1
    ObjectNode body1 = json.createObjectNode();
    body1.put("name", "Alice");
    body1.put("positionId", pos1);
    mockMvc
        .perform(
            put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body1.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.positionId").value(pos1))
        .andExpect(jsonPath("$.positionName").value("Backend Engineer"));
    // change to pos2
    ObjectNode body2 = json.createObjectNode();
    body2.put("name", "Alice");
    body2.put("positionId", pos2);
    mockMvc
        .perform(
            put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body2.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.positionId").value(pos2))
        .andExpect(jsonPath("$.positionName").value("Product Owner"))
        .andExpect(jsonPath("$.positionCategory").value("PM"));
  }

  /** TC-USR-004: PUT positionId=null 清空岗位. */
  @Test
  void put_nullPositionId_clearsPosition() throws Exception {
    Long pos = createPosition("BE_ENG", "Backend Engineer", "TECH");
    Long userId = createWithFlags("alice", "Alice", true);
    ObjectNode setBody = json.createObjectNode();
    setBody.put("name", "Alice");
    setBody.put("positionId", pos);
    mockMvc
        .perform(
            put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(setBody.toString()))
        .andExpect(status().isOk());
    ObjectNode clearBody = json.createObjectNode();
    clearBody.put("name", "Alice");
    clearBody.putNull("positionId");
    mockMvc
        .perform(
            put("/api/users/" + userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(clearBody.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.positionId").isEmpty())
        .andExpect(jsonPath("$.positionName").isEmpty())
        .andExpect(jsonPath("$.positionCategory").isEmpty());
  }
}
