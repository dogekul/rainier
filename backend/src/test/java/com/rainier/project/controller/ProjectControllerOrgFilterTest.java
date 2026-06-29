/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * v0.0.108 (H1) — TC-PRJ-ORG-001/002: GET /api/projects?organizationId=X filter.
 *
 * <p>Mirrors the {@code status=ACTIVE} filter style in {@link ProjectControllerQueryTest}: seed
 * mixed-org rows, assert {@code total} and {@code content[*].organizationId} both align.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerOrgFilterTest {

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

  private void createProject(Long ownerId, String name, Long organizationId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("name", name);
    body.put("ownerUserId", ownerId);
    if (organizationId != null) {
      body.put("organizationId", organizationId);
    }
    mockMvc
        .perform(
            post("/api/projects")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
  }

  /** TC-PRJ-ORG-001: 传 organizationId 仅返回挂该 org 的项目. */
  @Test
  void getList_filterByOrganizationId_returnsOnlyMatching() throws Exception {
    Long userId = createUser();
    createProject(userId, "P1", 10L);
    createProject(userId, "P2", 10L);
    createProject(userId, "P3", 20L);
    mockMvc
        .perform(get("/api/projects?organizationId=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].organizationId", everyItem(is(10))));
  }

  /** TC-PRJ-ORG-002: 不传 organizationId 返回全部（既有行为不变）. */
  @Test
  void getList_noOrganizationIdParam_returnsAll() throws Exception {
    Long userId = createUser();
    createProject(userId, "P1", 10L);
    createProject(userId, "P2", 10L);
    createProject(userId, "P3", 20L);
    mockMvc
        .perform(get("/api/projects"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3));
  }
}
