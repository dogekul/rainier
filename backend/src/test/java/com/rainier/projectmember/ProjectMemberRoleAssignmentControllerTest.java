/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.controller.AuthController;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.projectmember.repository.ProjectMemberRoleAssignmentRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.88 (C8) — /api/project-members/{id}/roles add / list / remove + 鉴权. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMemberRoleAssignmentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectMemberRepository repo;
  @Autowired private ProjectMemberRoleAssignmentRepository roleRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long projectId;
  private Long memberId;
  private Long ownerId;
  private Long devUserId;
  private Long bobId;

  @BeforeEach
  void cleanDb() {
    roleRepo.deleteAll();
    repo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();

    ownerId = newUser("lina", "李娜");
    devUserId = newUser("dev1", "DevA");
    bobId = newUser("bob", "Bob");

    Project p = new Project();
    p.setCode("ED-1");
    p.setName("Test");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    p.setOwnerUserId(ownerId);
    p.setEnabled(Boolean.TRUE);
    projectId = projectRepo.saveAndFlush(p).getId();

    ProjectMember m = new ProjectMember();
    m.setProjectId(projectId);
    m.setUserId(devUserId);
    m.setRole("DEV");
    m.setJoinedAt(Instant.now());
    m.setJoinedBy("lina");
    memberId = repo.saveAndFlush(m).getId();
  }

  private Long newUser(String login, String name) {
    User u = new User();
    u.setLoginName(login);
    u.setName(name);
    u.setEnabled(Boolean.TRUE);
    return userRepo.saveAndFlush(u).getId();
  }

  private ObjectNode addBody(String role) {
    ObjectNode n = json.createObjectNode();
    n.put("projectRole", role);
    return n;
  }

  /** TC-PMR-001 add role 成功 → 201. */
  @Test
  void add_role_ok() throws Exception {
    mockMvc
        .perform(
            post("/api/project-members/" + memberId + "/roles")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBody("QA").toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.projectRole").value("QA"))
        .andExpect(jsonPath("$.projectMemberId").value(memberId));
  }

  /** TC-PMR-002 list roles 含 already-added. */
  @Test
  void list_roles() throws Exception {
    mockMvc.perform(
        post("/api/project-members/" + memberId + "/roles")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(addBody("QA").toString()));
    mockMvc
        .perform(get("/api/project-members/" + memberId + "/roles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].projectRole").value("QA"));
  }

  /** TC-PMR-003 remove role 成功. */
  @Test
  void remove_role_ok() throws Exception {
    mockMvc.perform(
        post("/api/project-members/" + memberId + "/roles")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(addBody("QA").toString()));
    mockMvc
        .perform(
            delete("/api/project-members/" + memberId + "/roles/QA")
                .requestAttr(AuthController.ATTR_USERNAME, "lina"))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/project-members/" + memberId + "/roles"))
        .andExpect(jsonPath("$.length()").value(0));
  }

  /** TC-PMR-004 非法 role → 400. */
  @Test
  void invalid_role_400() throws Exception {
    mockMvc
        .perform(
            post("/api/project-members/" + memberId + "/roles")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBody("FAKE").toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-PMR-005 重复 role → 409. */
  @Test
  void duplicate_role_409() throws Exception {
    mockMvc.perform(
        post("/api/project-members/" + memberId + "/roles")
            .requestAttr(AuthController.ATTR_USERNAME, "lina")
            .contentType(MediaType.APPLICATION_JSON)
            .content(addBody("QA").toString()));
    mockMvc
        .perform(
            post("/api/project-members/" + memberId + "/roles")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBody("QA").toString()))
        .andExpect(status().isConflict());
  }

  /** TC-PMR-006 非授权 → 403. */
  @Test
  void unauthorized_403() throws Exception {
    mockMvc
        .perform(
            post("/api/project-members/" + memberId + "/roles")
                .requestAttr(AuthController.ATTR_USERNAME, "bob")
                .contentType(MediaType.APPLICATION_JSON)
                .content(addBody("QA").toString()))
        .andExpect(status().isForbidden());
  }
}
