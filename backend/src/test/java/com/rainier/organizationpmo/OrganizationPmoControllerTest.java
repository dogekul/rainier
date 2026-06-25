/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.controller.AuthController;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.organizationpmo.repository.OrganizationPmoRepository;
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

/** v0.0.64 — Organization↔PMO 关系 + 继承 + 权限. Covers TC-OPMO-001~008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationPmoControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationPmoRepository repo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private ObjectMapper json;

  private Long rootOrgId;
  private Long midOrgId;
  private Long leafOrgId;
  private Long aliceId;
  private Long lilingId;
  private Long bobId;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    userRoleRepo.deleteAll();
    orgRepo.deleteAll();
    roleRepo.deleteAll();
    userRepo.deleteAll();

    // 3-level org tree: root → mid → leaf
    Organization root = newOrg("ROOT", "招联金融", null);
    rootOrgId = orgRepo.saveAndFlush(root).getId();
    Organization mid = newOrg("MID", "研发中心", rootOrgId);
    midOrgId = orgRepo.saveAndFlush(mid).getId();
    Organization leaf = newOrg("LEAF", "采购研发团队", midOrgId);
    leafOrgId = orgRepo.saveAndFlush(leaf).getId();

    // users + admin role
    aliceId = newUser("alice", "Alice");
    lilingId = newUser("liling", "黎立");
    bobId = newUser("bob", "Bob");

    Role admin = new Role();
    admin.setCode("ADMIN");
    admin.setName("Admin");
    admin.setEnabled(Boolean.TRUE);
    admin.setAdminAccess(Boolean.TRUE);
    Long adminRoleId = roleRepo.saveAndFlush(admin).getId();

    UserRole ur = new UserRole();
    ur.setUserId(aliceId);
    ur.setRoleId(adminRoleId);
    userRoleRepo.saveAndFlush(ur);
  }

  private Organization newOrg(String code, String name, Long parent) {
    Organization o = new Organization();
    o.setCode(code);
    o.setName(name);
    o.setParentId(parent);
    o.setType(OrganizationType.DEPARTMENT);
    o.setEnabled(Boolean.TRUE);
    return o;
  }

  private Long newUser(String login, String name) {
    User u = new User();
    u.setLoginName(login);
    u.setName(name);
    u.setEnabled(Boolean.TRUE);
    return userRepo.saveAndFlush(u).getId();
  }

  private ObjectNode body(Long uid) {
    ObjectNode n = json.createObjectNode();
    n.put("userId", uid);
    return n;
  }

  /** TC-OPMO-001 admin POST 加 PMO → 201 + DB 行 + enrichment. */
  @Test
  void admin_can_create_pmo() throws Exception {
    mockMvc
        .perform(
            post("/api/organizations/" + midOrgId + "/pmos")
                .requestAttr(AuthController.ATTR_USERNAME, "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(lilingId).toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.organizationId").value(midOrgId))
        .andExpect(jsonPath("$.userId").value(lilingId))
        .andExpect(jsonPath("$.userName").value("黎立"));
    assertEquals(1, repo.count());
  }

  /** TC-OPMO-002 重复添加 → 409. */
  @Test
  void duplicate_pmo_returns_409() throws Exception {
    mockMvc.perform(
        post("/api/organizations/" + midOrgId + "/pmos")
            .requestAttr(AuthController.ATTR_USERNAME, "alice")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(lilingId).toString()));
    mockMvc
        .perform(
            post("/api/organizations/" + midOrgId + "/pmos")
                .requestAttr(AuthController.ATTR_USERNAME, "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(lilingId).toString()))
        .andExpect(status().isConflict());
  }

  /** TC-OPMO-007 非 admin POST → 403. */
  @Test
  void non_admin_create_403() throws Exception {
    mockMvc
        .perform(
            post("/api/organizations/" + midOrgId + "/pmos")
                .requestAttr(AuthController.ATTR_USERNAME, "bob")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(lilingId).toString()))
        .andExpect(status().isForbidden());
  }

  /** TC-OPMO-004 子组织继承父组织 PMO + TC-OPMO-005 顶级只返自身. */
  @Test
  void leaf_org_inherits_ancestor_pmos() throws Exception {
    // Root has alice; mid has liling. Leaf has no own.
    mockMvc.perform(
        post("/api/organizations/" + rootOrgId + "/pmos")
            .requestAttr(AuthController.ATTR_USERNAME, "alice")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(aliceId).toString()));
    mockMvc.perform(
        post("/api/organizations/" + midOrgId + "/pmos")
            .requestAttr(AuthController.ATTR_USERNAME, "alice")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(lilingId).toString()));

    // Leaf effective-pmos: 2 entries (mid own first, root inherited second).
    mockMvc
        .perform(get("/api/organizations/" + leafOrgId + "/effective-pmos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].userId").value(lilingId))
        .andExpect(jsonPath("$[0].inheritedFromOrgId").value(midOrgId))
        .andExpect(jsonPath("$[1].userId").value(aliceId))
        .andExpect(jsonPath("$[1].inheritedFromOrgId").value(rootOrgId));

    // Root effective-pmos: just self.
    mockMvc
        .perform(get("/api/organizations/" + rootOrgId + "/effective-pmos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].inheritedFromOrgId").value(rootOrgId));
  }

  /** TC-OPMO-003 + TC-OPMO-006 删自身 OK; 在子 org 删 inherited 返 400. */
  @Test
  void delete_own_ok_but_inherited_400() throws Exception {
    mockMvc.perform(
        post("/api/organizations/" + rootOrgId + "/pmos")
            .requestAttr(AuthController.ATTR_USERNAME, "alice")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(aliceId).toString()));

    // Try to delete from leaf (inherited) → 400.
    mockMvc
        .perform(
            delete("/api/organizations/" + leafOrgId + "/pmos/" + aliceId)
                .requestAttr(AuthController.ATTR_USERNAME, "alice"))
        .andExpect(status().isBadRequest());

    // Delete from root (own) → 200.
    mockMvc
        .perform(
            delete("/api/organizations/" + rootOrgId + "/pmos/" + aliceId)
                .requestAttr(AuthController.ATTR_USERNAME, "alice"))
        .andExpect(status().isOk());
    // After delete, leaf effective-pmos no longer contains alice.
    mockMvc
        .perform(get("/api/organizations/" + leafOrgId + "/effective-pmos"))
        .andExpect(jsonPath("$.length()").value(0));
  }
}
