/* (C) 2026 Rainier — internal use only. */
package com.rainier.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
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

/** v0.0.20: GET /api/auth/me exposes roles[].adminAccess. Covers TC-ME-ADM-001/002. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthMeAdminAccessTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    roleRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedRole(String code, boolean adminAccess) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    r.setAdminAccess(adminAccess);
    return roleRepo.saveAndFlush(r).getId();
  }

  private void link(Long userId, Long roleId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(null);
    userRoleRepo.saveAndFlush(ur);
  }

  /** TC-ME-ADM-001: adminAccess=true 角色 → me roles[].adminAccess=true. */
  @Test
  void me_adminRole_returnsAdminAccessTrue() throws Exception {
    Long uid = seedUser("alice");
    Long rid = seedRole("ADMIN", true);
    link(uid, rid);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0].roleCode").value("ADMIN"))
        .andExpect(jsonPath("$.roles[0].adminAccess").value(true));
  }

  /** TC-ME-ADM-002: adminAccess=false 角色 → me roles[].adminAccess=false（非 null）. */
  @Test
  void me_nonAdminRole_returnsAdminAccessFalseNotNull() throws Exception {
    Long uid = seedUser("bob");
    Long rid = seedRole("DEV", false);
    link(uid, rid);
    String token = authService.issueToken("bob");

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.roles[0].roleCode").value("DEV"))
        .andExpect(jsonPath("$.roles[0].adminAccess").value(false));
  }
}
