/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

/** Integration tests for {@link RoleController} DELETE. Covers TC-ROL-007..008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    roleRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createRole() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PMO");
    body.put("name", "PMO");
    MvcResult res =
        mockMvc
            .perform(
                post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  /** TC-ROL-007: 无 user_role 引用软删成功。 */
  @Test
  void delete_noUserRoleRef_returns204AndGet404() throws Exception {
    Long id = createRole();
    mockMvc.perform(delete("/api/roles/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/roles/" + id)).andExpect(status().isNotFound());
  }

  /** TC-ROL-008: 有 user_role 引用被拒。 */
  @Test
  void delete_withUserRoleRef_returns409() throws Exception {
    Long roleId = createRole();
    Long userId = createUser();
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    userRoleRepo.saveAndFlush(ur);

    mockMvc
        .perform(delete("/api/roles/" + roleId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("role has assignments")));
  }
}
