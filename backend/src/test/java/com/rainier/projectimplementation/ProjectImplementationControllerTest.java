/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectimplementation.repository.ProjectImplementationRepository;
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

/** v0.0.89 — D1 project-implementation-form HTTP. Covers PIF-001/003/004/006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectImplementationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectImplementationRepository repo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long seedProject(String code) {
    User u = new User();
    u.setLoginName("pif-http-" + code);
    u.setName("pif-http-" + code);
    u.setIsInternal(true);
    u.setEnabled(true);
    Long uid = userRepo.saveAndFlush(u).getId();

    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(uid);
    p.setEnabled(true);
    p.setProjectType("CONTRACT");
    return projectRepo.saveAndFlush(p).getId();
  }

  /** PIF-003: GET 不存在 → 404. */
  @Test
  void get_missing_returns404() throws Exception {
    Long projectId = seedProject("PIF-HTTP-A");
    mockMvc
        .perform(get("/api/projects/" + projectId + "/implementation"))
        .andExpect(status().isNotFound());
  }

  /** PIF-001 + PIF-004: PUT 创建 → 200; GET 拿到. */
  @Test
  void put_then_get() throws Exception {
    Long projectId = seedProject("PIF-HTTP-B");
    ObjectNode body = json.createObjectNode();
    body.put("scopeMarkdown", "# 范围\n- 一期");
    body.put("estimatedManDays", 45);
    body.put("riskNotes", "网络隔离");
    mockMvc
        .perform(
            put("/api/projects/" + projectId + "/implementation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.projectId").value(projectId.intValue()))
        .andExpect(jsonPath("$.estimatedManDays").value(45));

    mockMvc
        .perform(get("/api/projects/" + projectId + "/implementation"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.scopeMarkdown").value("# 范围\n- 一期"));
  }

  /** PIF-002: PUT 二次 → 同 id（upsert）. */
  @Test
  void put_twice_isUpsert() throws Exception {
    Long projectId = seedProject("PIF-HTTP-C");
    ObjectNode body1 = json.createObjectNode();
    body1.put("scopeMarkdown", "v1");
    String first =
        mockMvc
            .perform(
                put("/api/projects/" + projectId + "/implementation")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body1.toString()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    Long firstId = json.readTree(first).get("id").asLong();

    ObjectNode body2 = json.createObjectNode();
    body2.put("scopeMarkdown", "v2");
    body2.put("estimatedManDays", 30);
    mockMvc
        .perform(
            put("/api/projects/" + projectId + "/implementation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body2.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(firstId.intValue()))
        .andExpect(jsonPath("$.scopeMarkdown").value("v2"))
        .andExpect(jsonPath("$.estimatedManDays").value(30));
  }

  /** PIF-006: PUT with blank scopeMarkdown → 400. */
  @Test
  void put_blankScope_returns400() throws Exception {
    Long projectId = seedProject("PIF-HTTP-D");
    ObjectNode body = json.createObjectNode();
    body.put("scopeMarkdown", "   ");
    mockMvc
        .perform(
            put("/api/projects/" + projectId + "/implementation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }
}
