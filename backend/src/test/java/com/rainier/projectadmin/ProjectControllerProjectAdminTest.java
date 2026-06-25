/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectadmin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.controller.AuthController;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectadmin.service.ProjectAdminService;
import com.rainier.projectmember.repository.ProjectMemberRepository;
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

/**
 * v0.0.78 (B5) — ProjectController.update/delete + ProjectAdminController grant/revoke. 覆盖 TC-PCTRL-PA-001..008.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerProjectAdminTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectMemberRepository memberRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private ProjectAdminService projectAdminService;
  @Autowired private ObjectMapper json;

  private Long projectAId;
  private Long projectBId;
  private Long ownerAId;
  private Long ownerBId;
  private Long pmoAId;
  private Long projAdminId;
  private Long randomId;
  private Long globalAdminId;

  @BeforeEach
  void cleanDb() {
    memberRepo.deleteAll();
    projectRepo.deleteAll();
    userRoleRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();

    ownerAId = newUser("owner-a", "Owner A");
    ownerBId = newUser("owner-b", "Owner B");
    pmoAId = newUser("pmo-a", "Pmo A");
    projAdminId = newUser("padmin-1", "ProjAdmin");
    randomId = newUser("rando-1", "Rando");
    globalAdminId = newUser("gadmin-1", "GlobalAdmin");

    Role admin = new Role();
    admin.setCode("ADMIN");
    admin.setName("Admin");
    admin.setEnabled(Boolean.TRUE);
    admin.setAdminAccess(Boolean.TRUE);
    Long adminRoleId = roleRepo.saveAndFlush(admin).getId();
    UserRole ur = new UserRole();
    ur.setUserId(globalAdminId);
    ur.setRoleId(adminRoleId);
    userRoleRepo.saveAndFlush(ur);

    projectAId = newProject("PAA-1", ownerAId, pmoAId);
    projectBId = newProject("PAB-2", ownerBId, null);

    // grant projAdminId as project-admin of projectA
    projectAdminService.updateGrant(projectAId, projAdminId, "owner-a");
  }

  private Long newUser(String login, String name) {
    User u = new User();
    u.setLoginName(login);
    u.setName(name);
    u.setEnabled(Boolean.TRUE);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long newProject(String code, Long ownerId, Long pmoUserId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus(ProjectStatus.ACTIVE);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    p.setOwnerUserId(ownerId);
    p.setPmoUserId(pmoUserId);
    p.setEnabled(Boolean.TRUE);
    return projectRepo.saveAndFlush(p).getId();
  }

  private ObjectNode updateBody(Long projectId, Long ownerId) {
    Project p = projectRepo.findById(projectId).get();
    ObjectNode n = json.createObjectNode();
    n.put("code", p.getCode());
    n.put("name", p.getName());
    n.put("status", p.getStatus());
    n.put("ownerUserId", ownerId);
    n.put("enabled", true);
    return n;
  }

  /** TC-PCTRL-PA-001 项目管理员 PUT own project → 200. */
  @Test
  void project_admin_can_update_own_project() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectAId)
                .requestAttr(AuthController.ATTR_USERNAME, "padmin-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectAId, ownerAId).toString()))
        .andExpect(status().isOk());
  }

  /** TC-PCTRL-PA-002 项目管理员 PUT other project → 403. */
  @Test
  void project_admin_cannot_update_other_project() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectBId)
                .requestAttr(AuthController.ATTR_USERNAME, "padmin-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectBId, ownerBId).toString()))
        .andExpect(status().isForbidden());
  }

  /** TC-PCTRL-PA-003 random 登录用户 PUT → 403. */
  @Test
  void random_user_cannot_update_project() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectAId)
                .requestAttr(AuthController.ATTR_USERNAME, "rando-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectAId, ownerAId).toString()))
        .andExpect(status().isForbidden());
  }

  /** TC-PCTRL-PA-004 global admin PUT 任意 project → 200. */
  @Test
  void global_admin_can_update_any_project() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectBId)
                .requestAttr(AuthController.ATTR_USERNAME, "gadmin-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectBId, ownerBId).toString()))
        .andExpect(status().isOk());
  }

  /** TC-PCTRL-PA-005 owner PUT own → 200 (既有契约保持). */
  @Test
  void owner_can_update_own_project() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectAId)
                .requestAttr(AuthController.ATTR_USERNAME, "owner-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectAId, ownerAId).toString()))
        .andExpect(status().isOk());
  }

  /** TC-PCTRL-PA-005b pmo PUT own → 200. */
  @Test
  void pmo_can_update_own_project() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectAId)
                .requestAttr(AuthController.ATTR_USERNAME, "pmo-a")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectAId, ownerAId).toString()))
        .andExpect(status().isOk());
  }

  /** TC-PCTRL-PA-006 POST /api/projects/{id}/admins/{uid} 非 admin → 403. */
  @Test
  void grant_admin_requires_global_admin() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectAId + "/admins/" + randomId)
                .requestAttr(AuthController.ATTR_USERNAME, "owner-a"))
        .andExpect(status().isForbidden());
  }

  /** TC-PCTRL-PA-007 global admin grant → 200 + list contains userId. */
  @Test
  void global_admin_can_grant() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectBId + "/admins/" + randomId)
                .requestAttr(AuthController.ATTR_USERNAME, "gadmin-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0]").value(randomId));
  }

  /** TC-PCTRL-PA-008 global admin revoke → 200 + 已撤销. */
  @Test
  void global_admin_can_revoke() throws Exception {
    mockMvc
        .perform(
            delete("/api/projects/" + projectAId + "/admins/" + projAdminId)
                .requestAttr(AuthController.ATTR_USERNAME, "gadmin-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** anonymous (无 ATTR_USERNAME) PUT → 200（test profile / 不带 token 时不额外加锁，与既有契约一致）。 */
  @Test
  void anonymous_put_still_allowed_in_test_profile() throws Exception {
    mockMvc
        .perform(
            put("/api/projects/" + projectAId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(projectAId, ownerAId).toString()))
        .andExpect(status().isOk());
  }
}
