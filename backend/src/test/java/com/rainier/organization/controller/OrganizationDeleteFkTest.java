/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Covers TC-ORG-016. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationDeleteFkTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private UserOrganizationRepository uoRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    uoRepo.deleteAll();
    userRepo.deleteAll();
    orgRepo.deleteAll();
  }

  @Test
  void delete_organizationWithActiveUserAssignment_returns409() throws Exception {
    Long orgId = createOrg("TEAM-X", "X 团队");
    Long userId = createUser("alice", "Alice");
    createUserOrg(userId, orgId);

    mockMvc
        .perform(delete("/api/organizations/" + orgId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("has assigned users"));
  }

  private Long createOrg(String code, String name) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", "TEAM");
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

  private Long createUser(String loginName, String name) throws Exception {
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

  private void createUserOrg(Long userId, Long orgId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("organizationId", orgId);
    body.put("role", "MEMBER");
    mockMvc
        .perform(
            post("/api/user-organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  private Long readId(MvcResult r) throws Exception {
    JsonNode node = json.readTree(r.getResponse().getContentAsString());
    return node.get("id").asLong();
  }
}
