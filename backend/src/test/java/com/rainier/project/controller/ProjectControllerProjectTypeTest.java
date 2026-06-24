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

/** TC-PROJTYPE-001..008 — Project projectType create / update(转化) / list filter / detail. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerProjectTypeTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
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

  /** POST a project, optionally with projectType; return its id. */
  private Long createProject(Long ownerId, String code, String projectType) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", "X");
    body.put("ownerUserId", ownerId);
    if (projectType != null) {
      body.put("projectType", projectType);
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

  /** TC-PROJTYPE-001: 省略 projectType → 默认 CASUAL. */
  @Test
  void post_omitProjectType_defaultsCasual() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PT-001");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectType").value("CASUAL"));
  }

  /** TC-PROJTYPE-002: 显式 CORE_FEATURE. */
  @Test
  void post_explicitFormal_persisted() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PT-002");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    body.put("projectType", "CORE_FEATURE");
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectType").value("CORE_FEATURE"));
  }

  /** TC-PROJTYPE-003: 非法 projectType → 400. */
  @Test
  void post_invalidProjectType_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PT-003");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    body.put("projectType", "XXX");
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid project type")));
  }

  /** TC-PROJTYPE-004: update CASUAL→CORE_FEATURE 完成转化. */
  @Test
  void put_casualToFormal_converts() throws Exception {
    Long userId = createUser();
    Long id = createProject(userId, "PT-004", null); // default CASUAL
    ObjectNode body = json.createObjectNode();
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", userId);
    body.put("projectType", "CORE_FEATURE");
    mockMvc
        .perform(
            put("/api/projects/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectType").value("CORE_FEATURE"));
  }

  /** TC-PROJTYPE-005: update 省略 projectType → 保留原值（防静默降级）. */
  @Test
  void put_omitProjectType_preservesExisting() throws Exception {
    Long userId = createUser();
    Long id = createProject(userId, "PT-005", "CORE_FEATURE"); // created CORE_FEATURE
    ObjectNode body = json.createObjectNode();
    body.put("name", "X-renamed");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", userId);
    // NB: no projectType in payload — must NOT downgrade CORE_FEATURE → CASUAL.
    mockMvc
        .perform(
            put("/api/projects/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectType").value("CORE_FEATURE"));
  }

  /** TC-PROJTYPE-006: update 非法 projectType → 400. */
  @Test
  void put_invalidProjectType_returns400() throws Exception {
    Long userId = createUser();
    Long id = createProject(userId, "PT-006", null);
    ObjectNode body = json.createObjectNode();
    body.put("name", "X");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", userId);
    body.put("projectType", "XXX");
    mockMvc
        .perform(
            put("/api/projects/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid project type")));
  }

  /** TC-PROJTYPE-007: 按 projectType 过滤列表. */
  @Test
  void getList_filterByProjectType_returnsOnlyMatching() throws Exception {
    Long userId = createUser();
    createProject(userId, "PT-007a", "CORE_FEATURE");
    createProject(userId, "PT-007b", "CORE_FEATURE");
    createProject(userId, "PT-007c", "CASUAL");
    mockMvc
        .perform(get("/api/projects?projectType=CORE_FEATURE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].projectType", everyItem(is("CORE_FEATURE"))));
  }

  /** TC-PROJTYPE-008: 详情含 projectType（create-with-CORE_FEATURE round-trips to the detail read）. */
  @Test
  void get_detail_includesProjectType() throws Exception {
    Long userId = createUser();
    Long id = createProject(userId, "PT-008", "CORE_FEATURE");
    mockMvc
        .perform(get("/api/projects/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectType").value("CORE_FEATURE"));
  }

  /** TC-PROJTYPE-011 (v0.0.48): 对外-交付 EXTERNAL_DELIVERY 为合法类型. */
  @Test
  void post_externalDelivery_persisted() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PT-009");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    body.put("projectType", "EXTERNAL_DELIVERY");
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectType").value("EXTERNAL_DELIVERY"));
  }

  /** TC-PROJTYPE-012 (v0.0.48): 退役的 FORMAL 不再是合法 create 类型 → 400. */
  @Test
  void post_retiredFormal_returns400() throws Exception {
    Long userId = createUser();
    ObjectNode body = json.createObjectNode();
    body.put("code", "PT-010");
    body.put("name", "X");
    body.put("ownerUserId", userId);
    body.put("projectType", "FORMAL");
    mockMvc
        .perform(
            post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid project type")));
  }
}
