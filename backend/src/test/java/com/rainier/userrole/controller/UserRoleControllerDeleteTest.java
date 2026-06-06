/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/** Integration test for {@link UserRoleController} DELETE. Covers TC-UROL-009. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRoleControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();
  }

  /** TC-UROL-009: 硬删 → 204 + 后续 GET 404 + DB count=0. */
  @Test
  void delete_returnsNoContentAndRowVanishes() throws Exception {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    Long userId = userRepo.saveAndFlush(u).getId();

    Role r = new Role();
    r.setCode("PMO");
    r.setName("PMO");
    r.setEnabled(true);
    Long roleId = roleRepo.saveAndFlush(r).getId();

    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(42L);
    Long urId = userRoleRepo.saveAndFlush(ur).getId();

    mockMvc.perform(delete("/api/user-roles/" + urId)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/user-roles/" + urId)).andExpect(status().isNotFound());
    org.junit.jupiter.api.Assertions.assertEquals(0, userRoleRepo.count());
  }
}
