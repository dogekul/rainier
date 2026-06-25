/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectadmin.service.ProjectAdminService;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.78 (B5) — GET /api/auth/me 包含 adminProjectIds 字段. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeAdminProjectsTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private ProjectMemberRepository memberRepo;
  @Autowired private ProjectAdminService projectAdminService;

  @BeforeEach
  void cleanDb() {
    memberRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  /** TC-ME-ADMINPROJECTS-001: user 是 2 个项目的项目管理员 → adminProjectIds size = 2. */
  @Test
  void me_returns_admin_project_ids() throws Exception {
    User u = new User();
    u.setLoginName("padmin-me");
    u.setName("Me");
    u.setEnabled(Boolean.TRUE);
    Long uid = userRepo.saveAndFlush(u).getId();

    Long p1 = newProject("ME-P1", uid);
    Long p2 = newProject("ME-P2", uid);
    Long p3 = newProject("ME-P3", uid);

    projectAdminService.updateGrant(p1, uid, "system");
    projectAdminService.updateGrant(p2, uid, "system");
    // not p3

    mockMvc
        .perform(get("/api/auth/me").requestAttr(AuthController.ATTR_USERNAME, "padmin-me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adminProjectIds", hasSize(2)));
  }

  /** non-admin user → adminProjectIds is empty list (not missing / null). */
  @Test
  void me_empty_admin_project_ids() throws Exception {
    User u = new User();
    u.setLoginName("plain-me");
    u.setName("Plain");
    u.setEnabled(Boolean.TRUE);
    userRepo.saveAndFlush(u);

    mockMvc
        .perform(get("/api/auth/me").requestAttr(AuthController.ATTR_USERNAME, "plain-me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adminProjectIds", hasSize(0)));
  }

  private Long newProject(String code, Long ownerId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus(ProjectStatus.ACTIVE);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    p.setOwnerUserId(ownerId);
    p.setEnabled(Boolean.TRUE);
    return projectRepo.saveAndFlush(p).getId();
  }
}
