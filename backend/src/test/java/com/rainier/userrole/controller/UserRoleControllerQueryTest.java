/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Integration test for {@link UserRoleController} GET list 富化. Covers TC-UROL-008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRoleControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();
  }

  /** TC-UROL-008: GET 列表富化 userName/roleName. */
  @Test
  void getList_includesEnrichedUserAndRoleFields() throws Exception {
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

    ObjectNode body = json.createObjectNode();
    body.put("userId", userId);
    body.put("roleId", roleId);
    body.putNull("projectId");
    mockMvc
        .perform(
            post("/api/user-roles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());

    mockMvc
        .perform(get("/api/user-roles?userId=" + userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].userName").value("Alice"))
        .andExpect(jsonPath("$.content[0].userLoginName").value("alice"))
        .andExpect(jsonPath("$.content[0].roleName").value("PMO"))
        .andExpect(jsonPath("$.content[0].roleCode").value("PMO"))
        .andExpect(jsonPath("$.content[0].userId").value(userId))
        .andExpect(jsonPath("$.content[0].roleId").value(roleId))
        .andExpect(jsonPath("$.content[0].projectId").isEmpty());
  }
}
