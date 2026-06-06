/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.domain.User;
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

/** Integration tests for GET/PUT {@link RequirementController}. Covers TC-REQ-004..007. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementRepository repo;
  @Autowired private DemandRepository demandRepo;
  @Autowired private DemandRequirementLinkRepository linkRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
    demandRepo.deleteAll();
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createReq(Long ownerId, String code) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", ownerId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-REQ-004: GET 详情完整字段集。 */
  @Test
  void get_existingId_returnsFullDetail() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-FULL");
    MvcResult res =
        mockMvc
            .perform(get("/api/requirements/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "title",
      "description",
      "ownerUserId",
      "status",
      "priority",
      "complexity",
      "projectId",
      "closeReason",
      "createTime",
      "updateTime",
      "createBy",
      "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-REQ-005: 按 projectId 过滤（占位字段查询）。 */
  @Test
  void getList_filterByProjectId_returnsOnlyMatching() throws Exception {
    Long userId = createUser();
    Long id1 = createReq(userId, "REQ-A");
    createReq(userId, "REQ-B");
    // Set projectId=42 on REQ-A via the entity directly (no API to set projectId before update).
    Requirement r = repo.findById(id1).get();
    r.setProjectId(42L);
    repo.saveAndFlush(r);
    mockMvc
        .perform(get("/api/requirements?projectId=42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].projectId").value(42));
  }

  /** TC-REQ-006: PUT 状态更新。 */
  @Test
  void put_updateStatus_persists() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-U");
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-U");
    body.put("title", "x");
    body.put("description", "x");
    body.put("status", "APPROVED");
    body.put("priority", "HIGH");
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("APPROVED"))
        .andExpect(jsonPath("$.priority").value("HIGH"));
  }

  /** TC-REQ-007: PUT body ownerUserId 静默忽略。 */
  @Test
  void put_withOwnerUserIdInBody_silentlyIgnored() throws Exception {
    Long userId = createUser();
    Long id = createReq(userId, "REQ-OWN");
    ObjectNode body = json.createObjectNode();
    body.put("code", "REQ-OWN");
    body.put("title", "x");
    body.put("description", "x");
    body.put("ownerUserId", 999_999L); // legacy/unknown field — must be ignored
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerUserId").value(userId));
  }
}
