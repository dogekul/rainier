/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** v0.0.64 — Project member CRUD + UNION 合成 + 权限. Covers TC-PMEM-001~012. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMemberControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectMemberRepository repo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private ObjectMapper json;

  private Long projectId;
  private Long ownerId;
  private Long pmoId;
  private Long devId;
  private Long bobId; // non-authorized

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    projectRepo.deleteAll();
    userRoleRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();

    ownerId = newUser("lina", "李娜");
    pmoId = newUser("liling", "黎立");
    devId = newUser("chenmin", "陈敏");
    bobId = newUser("bob", "Bob");

    Project p = new Project();
    p.setCode("ED-1");
    p.setName("Test Project");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    p.setOwnerUserId(ownerId);
    p.setPmoUserId(pmoId);
    p.setEnabled(Boolean.TRUE);
    projectId = projectRepo.saveAndFlush(p).getId();
  }

  private Long newUser(String login, String name) {
    User u = new User();
    u.setLoginName(login);
    u.setName(name);
    u.setEnabled(Boolean.TRUE);
    return userRepo.saveAndFlush(u).getId();
  }

  private ObjectNode body(Long uid, String role) {
    ObjectNode n = json.createObjectNode();
    n.put("userId", uid);
    n.put("role", role);
    return n;
  }

  /** TC-PMEM-001 owner POST member 成功 → 201. */
  @Test
  void owner_can_add_member() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(devId, "DEV").toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(devId))
        .andExpect(jsonPath("$.role").value("DEV"))
        .andExpect(jsonPath("$.displayLabel").value("研发"))
        .andExpect(jsonPath("$.joinedBy").value("lina"));
  }

  /** TC-PMEM-002 非法 role → 400. */
  @Test
  void invalid_role_400() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(devId, "FAKE").toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-PMEM-003 重复添加 → 409. */
  @Test
  void duplicate_member_409() throws Exception {
    mockMvc.perform(
        post("/api/projects/" + projectId + "/members")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(devId, "DEV").toString()));
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(devId, "QA").toString()))
        .andExpect(status().isConflict());
  }

  /** TC-PMEM-004 加 owner 为成员 → 400. */
  @Test
  void cannot_add_owner_as_member() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(ownerId, "DEV").toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-PMEM-005 非授权用户 POST → 403. */
  @Test
  void unauthorized_user_403() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "bob")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(devId, "DEV").toString()))
        .andExpect(status().isForbidden());
  }

  /** TC-PMEM-006 project PMO 可加成员. */
  @Test
  void project_pmo_can_add_member() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "liling")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(devId, "QA").toString()))
        .andExpect(status().isCreated());
  }

  /** TC-PMEM-007 admin 可加成员（admin 不是 owner 也不是 project pmo）. */
  @Test
  void admin_can_add_member() throws Exception {
    // Create an admin role and assign to bob
    Role admin = new Role();
    admin.setCode("ADMIN");
    admin.setName("Admin");
    admin.setEnabled(Boolean.TRUE);
    admin.setAdminAccess(Boolean.TRUE);
    Long adminRoleId = roleRepo.saveAndFlush(admin).getId();
    UserRole ur = new UserRole();
    ur.setUserId(bobId);
    ur.setRoleId(adminRoleId);
    userRoleRepo.saveAndFlush(ur);

    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "bob")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(devId, "DEV").toString()))
        .andExpect(status().isCreated());
  }

  /** TC-PMEM-008 改 role. */
  @Test
  void update_role() throws Exception {
    mockMvc.perform(
        post("/api/projects/" + projectId + "/members")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(devId, "DEV").toString()));
    ObjectNode upd = json.createObjectNode();
    upd.put("role", "QA");
    mockMvc
        .perform(
            put("/api/projects/" + projectId + "/members/" + devId)
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(upd.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("QA"));
  }

  /** TC-PMEM-009 删普通成员 OK. */
  @Test
  void delete_member_ok() throws Exception {
    mockMvc.perform(
        post("/api/projects/" + projectId + "/members")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(devId, "DEV").toString()));
    mockMvc
        .perform(
            delete("/api/projects/" + projectId + "/members/" + devId)
                .requestAttr(AuthController.ATTR_USERNAME, "lina"))
        .andExpect(status().isOk());
  }

  /** TC-PMEM-010 删 owner → 400. */
  @Test
  void delete_owner_400() throws Exception {
    mockMvc
        .perform(
            delete("/api/projects/" + projectId + "/members/" + ownerId)
                .requestAttr(AuthController.ATTR_USERNAME, "lina"))
        .andExpect(status().isBadRequest());
  }

  /** TC-PMEM-011 列表 UNION owner + pmo + 真实行；顺序对. */
  @Test
  void list_unions_owner_pmo_and_real() throws Exception {
    mockMvc.perform(
        post("/api/projects/" + projectId + "/members")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(devId, "DEV").toString()));
    mockMvc
        .perform(get("/api/projects/" + projectId + "/members"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].role").value("OWNER"))
        .andExpect(jsonPath("$[0].displayLabel").value("负责人"))
        .andExpect(jsonPath("$[0].userId").value(ownerId))
        .andExpect(jsonPath("$[1].role").value("PMO"))
        .andExpect(jsonPath("$[1].displayLabel").value("项目PMO"))
        .andExpect(jsonPath("$[1].userId").value(pmoId))
        .andExpect(jsonPath("$[2].role").value("DEV"))
        .andExpect(jsonPath("$[2].userId").value(devId));
  }

  /** TC-PMEM-012 pmo == owner 时不重复合成 PMO 行. */
  @Test
  void pmo_equals_owner_no_dup() throws Exception {
    Project p = projectRepo.findById(projectId).get();
    p.setPmoUserId(ownerId);
    projectRepo.saveAndFlush(p);
    mockMvc
        .perform(get("/api/projects/" + projectId + "/members"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].role").value("OWNER"));
  }
}
