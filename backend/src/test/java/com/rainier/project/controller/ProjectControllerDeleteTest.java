/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link ProjectController} DELETE. Covers TC-PRJ-011..013. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createProject(Long ownerId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PROJ-D");
    body.put("name", "x");
    body.put("ownerUserId", ownerId);
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

  private Long createRole() {
    Role r = new Role();
    r.setCode("PMO");
    r.setName("PMO");
    r.setEnabled(true);
    return roleRepo.saveAndFlush(r).getId();
  }

  /** TC-PRJ-011: DELETE 无引用 → 204. */
  @Test
  void delete_noRefs_returns204AndGet404() throws Exception {
    Long userId = createUser();
    Long pid = createProject(userId);
    mockMvc.perform(delete("/api/projects/" + pid)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/projects/" + pid)).andExpect(status().isNotFound());
  }

  /** TC-PRJ-012: DELETE 被 Requirement 引用 → 409. */
  @Test
  void delete_withRequirementRef_returns409() throws Exception {
    Long userId = createUser();
    Long pid = createProject(userId);
    Requirement r = new Requirement();
    r.setCode("REQ-PRJ-DEL");
    r.setTitle("x");
    r.setDescription("x");
    r.setOwnerUserId(userId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    r.setProjectId(pid);
    requirementRepo.saveAndFlush(r);

    mockMvc
        .perform(delete("/api/projects/" + pid))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("project has linked requirements")));
  }

  /** TC-PRJ-013: DELETE 被 UserRole 引用 → 409. */
  @Test
  void delete_withUserRoleRef_returns409() throws Exception {
    Long userId = createUser();
    Long pid = createProject(userId);
    Long roleId = createRole();
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(pid);
    userRoleRepo.saveAndFlush(ur);

    mockMvc
        .perform(delete("/api/projects/" + pid))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("project has assigned user-roles")));
  }
}
