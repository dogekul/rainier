/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** TC-ME-001..003 — GET /api/auth/me current-user context (TC-ME-004 = existing no-token test). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeContextTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRoleRepository userRoleRepo;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    projectRepo.deleteAll();
    roleRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedRole(String code) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    return roleRepo.saveAndFlush(r).getId();
  }

  private Long seedProject(String code, String name, Long ownerId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(name);
    p.setStatus("PLANNING");
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private void link(Long userId, Long roleId, Long projectId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(projectId);
    userRoleRepo.saveAndFlush(ur);
  }

  /** TC-ME-001: 已知用户返回 id/name/roles/projects. */
  @Test
  void me_knownUser_returnsContext() throws Exception {
    Long uid = seedUser("alice", "Alice");
    Long rid = seedRole("PMO");
    Long pid = seedProject("PRJ-1", "采购", uid);
    link(uid, rid, pid);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(uid))
        .andExpect(jsonPath("$.username").value("alice"))
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.roles[0].roleCode").value("PMO"))
        .andExpect(jsonPath("$.roles[0].projectId").value(pid))
        .andExpect(jsonPath("$.roles[0].projectName").value("采购"))
        .andExpect(jsonPath("$.projects[0].id").value(pid))
        .andExpect(jsonPath("$.projects[0].code").value("PRJ-1"));
  }

  /** TC-ME-005: 同一项目的两个角色 → projects 去重为 1. */
  @Test
  void me_twoRolesSameProject_dedupesProjects() throws Exception {
    Long uid = seedUser("alice", "Alice");
    Long r1 = seedRole("PMO");
    Long r2 = seedRole("DEV");
    Long pid = seedProject("PRJ-1", "采购", uid);
    link(uid, r1, pid);
    link(uid, r2, pid);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles", hasSize(2)))
        .andExpect(jsonPath("$.projects", hasSize(1)))
        .andExpect(jsonPath("$.projects[0].id").value(pid));
  }

  /** TC-ME-002: 组织级角色（projectId null）返回但不带出项目. */
  @Test
  void me_orgLevelRole_projectIdNull_noProject() throws Exception {
    Long uid = seedUser("alice", "Alice");
    Long rid = seedRole("ADMIN");
    link(uid, rid, null); // org-level role, no project
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0].roleCode").value("ADMIN"))
        .andExpect(jsonPath("$.roles[0].projectId").value(nullValue()))
        .andExpect(jsonPath("$.projects", hasSize(0)));
  }

  /** TC-ME-003: username 无对应 User 时降级（id null, roles/projects 空）. */
  @Test
  void me_unknownUsername_degradesGracefully() throws Exception {
    String token = authService.issueToken("ghost"); // no User seeded for "ghost"

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("ghost"))
        .andExpect(jsonPath("$.id").value(nullValue()))
        .andExpect(jsonPath("$.roles", hasSize(0)))
        .andExpect(jsonPath("$.projects", hasSize(0)));
  }
}
