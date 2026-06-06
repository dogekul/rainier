/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.project.repository.ProjectRepository;
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

/** Integration tests for {@link ProjectController} GET/PUT. Covers TC-PRJ-007..010. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createProject(Long ownerId, String code, String name, String status)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    body.put("ownerUserId", ownerId);
    if (status != null) {
      body.put("status", status);
    }
    MvcResult res =
        mockMvc
            .perform(
                post("/api/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-PRJ-007: GET 详情完整字段集 + 富化. */
  @Test
  void get_existingId_returnsFullDetailAndEnriched() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long id = createProject(userId, "PROJ-001", "采购系统改造", null);
    MvcResult res =
        mockMvc
            .perform(get("/api/projects/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.ownerName").value("Alice"))
            .andExpect(jsonPath("$.ownerLoginName").value("alice"))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "name",
      "description",
      "status",
      "ownerUserId",
      "ownerName",
      "ownerLoginName",
      "startDate",
      "endDate",
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

  /** TC-PRJ-008: 按 status 过滤列表. */
  @Test
  void getList_filterByStatus_returnsOnlyMatching() throws Exception {
    Long userId = createUser("alice", "Alice");
    createProject(userId, "P1", "x", "ACTIVE");
    createProject(userId, "P2", "x", "ACTIVE");
    createProject(userId, "P3", "x", "ARCHIVED");
    mockMvc
        .perform(get("/api/projects?status=ACTIVE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        // Spec: body.content 全部 status="ACTIVE" — tighten the assertion so a wrong-filter bug
        // returning total=2 with non-ACTIVE rows would be caught.
        .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));
  }

  /** TC-PRJ-009: PUT 转移 owner — 富化跟随. */
  @Test
  void put_transferOwner_enrichmentFollows() throws Exception {
    Long userA = createUser("alice", "Alice");
    Long userB = createUser("lili", "黎立");
    Long id = createProject(userA, "PROJ-O", "x", null);
    ObjectNode body = json.createObjectNode();
    body.put("name", "x");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", userB);
    mockMvc
        .perform(
            put("/api/projects/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownerUserId").value(userB))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  /** TC-PRJ-010: PUT 新 ownerUserId 不存在 → 400. */
  @Test
  void put_unknownNewOwner_returns400() throws Exception {
    Long userId = createUser("alice", "Alice");
    Long id = createProject(userId, "PROJ-O2", "x", null);
    ObjectNode body = json.createObjectNode();
    body.put("name", "x");
    body.put("status", "PLANNING");
    body.put("ownerUserId", 999_999L);
    mockMvc
        .perform(
            put("/api/projects/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("owner user not found")));
  }
}
