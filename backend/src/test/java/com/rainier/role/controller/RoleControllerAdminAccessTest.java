/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.20: Role.adminAccess CRUD + legacy-NULL read-coalesce. Covers TC-ROLE-ADM-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerAdminAccessTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RoleRepository repo;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  /** TC-ROLE-ADM-001: create 不带 adminAccess → 默认 false. */
  @Test
  void post_noAdminAccess_defaultsFalse() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "PMO");
    body.put("name", "PMO");
    mockMvc
        .perform(
            post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.adminAccess").value(false));
    // AND the persisted row reads adminAccess=false (not just the serialized response).
    org.junit.jupiter.api.Assertions.assertFalse(repo.findAll().get(0).getAdminAccess());
  }

  /** TC-ROLE-ADM-002: create adminAccess:true → 持久为 true. */
  @Test
  void post_adminAccessTrue_persistsTrue() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "ADMIN");
    body.put("name", "管理员");
    body.put("adminAccess", true);
    mockMvc
        .perform(
            post("/api/roles").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.adminAccess").value(true));
    // AND the persisted row reads adminAccess=true.
    org.junit.jupiter.api.Assertions.assertTrue(repo.findAll().get(0).getAdminAccess());
  }

  /** TC-ROLE-ADM-003: update 切换 adminAccess false→true. */
  @Test
  void put_togglesAdminAccess() throws Exception {
    Role r = new Role();
    r.setCode("PMO");
    r.setName("PMO");
    r.setEnabled(true);
    r.setAdminAccess(false);
    Long id = repo.saveAndFlush(r).getId();

    ObjectNode body = json.createObjectNode();
    body.put("name", "PMO");
    body.put("adminAccess", true);
    mockMvc
        .perform(
            put("/api/roles/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adminAccess").value(true));
  }

  /** TC-ROLE-ADM-004: 存量行 admin_access NULL → 读为 false（不被写动）. */
  @Test
  void get_legacyNullAdminAccess_readsFalse() throws Exception {
    Role r = new Role();
    r.setCode("LEGACY");
    r.setName("Legacy");
    r.setEnabled(true);
    Long id = repo.saveAndFlush(r).getId();
    // Force a pre-v0.0.20 NULL into the column (auto-commits — visible to the MockMvc read).
    jdbc.update("UPDATE rainier_role SET admin_access = NULL WHERE id = ?", id);

    mockMvc
        .perform(get("/api/roles/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.adminAccess").value(false));

    // The read SHALL NOT have mutated the stored value — it is still NULL.
    Object stored =
        jdbc.queryForObject("SELECT admin_access FROM rainier_role WHERE id = ?", Object.class, id);
    org.junit.jupiter.api.Assertions.assertNull(stored);
  }
}
