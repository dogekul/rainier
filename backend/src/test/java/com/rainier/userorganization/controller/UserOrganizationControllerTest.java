/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link UserOrganizationController}. Covers TC-UOR-001..010. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserOrganizationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserOrganizationRepository uoRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    uoRepo.deleteAll();
    userRepo.deleteAll();
    orgRepo.deleteAll();
  }

  @Test
  void post_validPayload_returns201WithLeftAtNull() throws Exception {
    String userId = createUser("alice", "Alice");
    String orgId = createOrg("HQ", "总公司");
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("organizationId", orgId);
    mockMvc
        .perform(
            post("/api/user-organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.organizationId").value(orgId))
        .andExpect(jsonPath("$.role").value("MEMBER"))
        .andExpect(jsonPath("$.isPrimary").value(false))
        .andExpect(jsonPath("$.leftAt").isEmpty())
        .andExpect(jsonPath("$.joinedAt").exists());
  }

  @Test
  void post_duplicatePair_returns409() throws Exception {
    String userId = createUser("alice", "Alice");
    String orgId = createOrg("HQ", "总公司");
    createUserOrg(userId, orgId, "MEMBER", false);
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("organizationId", orgId);
    mockMvc
        .perform(
            post("/api/user-organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict());
  }

  @Test
  void post_userNotFound_returns400() throws Exception {
    String orgId = createOrg("HQ", "总公司");
    ObjectNode body = json.createObjectNode();
    body.put("userId", "ghost00000000000000000000000000");
    body.put("organizationId", orgId);
    mockMvc
        .perform(
            post("/api/user-organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message").value("user not found: id=ghost00000000000000000000000000"));
  }

  @Test
  void post_isPrimaryTrue_demotesPreviousPrimary() throws Exception {
    String userId = createUser("alice", "Alice");
    String orgA = createOrg("A", "公司A");
    String orgB = createOrg("B", "公司B");
    String firstUoId = createUserOrg(userId, orgA, "MEMBER", true);
    String secondUoId = createUserOrg(userId, orgB, "MEMBER", true);

    mockMvc
        .perform(get("/api/user-organizations/" + secondUoId))
        .andExpect(jsonPath("$.isPrimary").value(true));
    mockMvc
        .perform(get("/api/user-organizations/" + firstUoId))
        .andExpect(jsonPath("$.isPrimary").value(false));
  }

  @Test
  void getList_filterByUserId() throws Exception {
    String alice = createUser("alice", "Alice");
    String bob = createUser("bob", "Bob");
    String orgA = createOrg("A", "公司A");
    String orgB = createOrg("B", "公司B");
    createUserOrg(alice, orgA, "MEMBER", true);
    createUserOrg(alice, orgB, "MEMBER", false);
    createUserOrg(bob, orgA, "MEMBER", true);

    mockMvc
        .perform(get("/api/user-organizations?userId=" + alice))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void getList_filterByOrganizationId() throws Exception {
    String alice = createUser("alice", "Alice");
    String bob = createUser("bob", "Bob");
    String orgA = createOrg("A", "公司A");
    String orgB = createOrg("B", "公司B");
    createUserOrg(alice, orgA, "MEMBER", true);
    createUserOrg(bob, orgA, "HEAD", true);
    createUserOrg(alice, orgB, "MEMBER", false);

    mockMvc
        .perform(get("/api/user-organizations?organizationId=" + orgA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void getList_includesEnrichment() throws Exception {
    String alice = createUser("alice", "Alice");
    String hq = createOrg("HQ", "总公司");
    createUserOrg(alice, hq, "HEAD", true);
    mockMvc
        .perform(get("/api/user-organizations?userId=" + alice))
        .andExpect(jsonPath("$.content[0].userLoginName").value("alice"))
        .andExpect(jsonPath("$.content[0].userName").value("Alice"))
        .andExpect(jsonPath("$.content[0].organizationName").value("总公司"))
        .andExpect(jsonPath("$.content[0].organizationType").value("COMPANY"));
  }

  @Test
  void put_setLeftAt_marksHistorical() throws Exception {
    String userId = createUser("alice", "Alice");
    String orgId = createOrg("HQ", "总公司");
    String uoId = createUserOrg(userId, orgId, "MEMBER", false);

    ObjectNode body = json.createObjectNode();
    body.put("leftAt", "2026-12-31T23:59:59Z");
    mockMvc
        .perform(
            put("/api/user-organizations/" + uoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.leftAt").value("2026-12-31T23:59:59Z"));
  }

  @Test
  void put_promoteRoleToHead() throws Exception {
    String userId = createUser("alice", "Alice");
    String orgId = createOrg("HQ", "总公司");
    String uoId = createUserOrg(userId, orgId, "MEMBER", false);

    ObjectNode body = json.createObjectNode();
    body.put("role", "HEAD");
    mockMvc
        .perform(
            put("/api/user-organizations/" + uoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("HEAD"));
  }

  @Test
  void delete_hardDeletes_subsequentGetIs404() throws Exception {
    String userId = createUser("alice", "Alice");
    String orgId = createOrg("HQ", "总公司");
    String uoId = createUserOrg(userId, orgId, "MEMBER", false);

    mockMvc.perform(delete("/api/user-organizations/" + uoId)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/user-organizations/" + uoId)).andExpect(status().isNotFound());
  }

  // ---------------------------- helpers ----------------------------------

  private String createUser(String loginName, String name) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("loginName", loginName);
    body.put("name", name);
    MvcResult r =
        mockMvc
            .perform(
                post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return readId(r);
  }

  private String createOrg(String code, String name) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", "COMPANY");
    body.put("code", code);
    body.put("name", name);
    MvcResult r =
        mockMvc
            .perform(
                post("/api/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return readId(r);
  }

  private String createUserOrg(String userId, String orgId, String role, boolean isPrimary)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("organizationId", orgId);
    body.put("role", role);
    body.put("isPrimary", isPrimary);
    MvcResult r =
        mockMvc
            .perform(
                post("/api/user-organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return readId(r);
  }

  private String readId(MvcResult r) throws Exception {
    JsonNode node = json.readTree(r.getResponse().getContentAsString());
    return node.get("id").asText();
  }
}
