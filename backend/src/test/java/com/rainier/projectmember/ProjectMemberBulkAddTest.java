/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.controller.AuthController;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.projectmember.repository.ProjectMemberRoleAssignmentRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.88 (C8) — bulk add 项目成员 × 多角色 / merge / owner-skip / 富化 roles[]. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMemberBulkAddTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProjectMemberRepository repo;
  @Autowired private ProjectMemberRoleAssignmentRepository roleRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long projectId;
  private Long ownerId;
  private Long u1;
  private Long u2;
  private Long u3;

  @BeforeEach
  void cleanDb() {
    roleRepo.deleteAll();
    repo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
    ownerId = newUser("lina", "李娜");
    u1 = newUser("u1", "用户1");
    u2 = newUser("u2", "用户2");
    u3 = newUser("u3", "用户3");

    Project p = new Project();
    p.setCode("ED-1");
    p.setName("Test");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    p.setOwnerUserId(ownerId);
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

  private ObjectNode bulkBody(Long[] userIds, String[] roles) {
    ObjectNode n = json.createObjectNode();
    ArrayNode arr = n.putArray("memberUserIds");
    for (Long id : userIds) arr.add(id);
    ArrayNode rs = n.putArray("projectRoles");
    for (String r : roles) rs.add(r);
    return n;
  }

  /** TC-PMEMB-001 bulk add 3 人 × 2 role → 3 ProjectMember + 6 RoleAssignment. */
  @Test
  void bulk_add_cartesian() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    bulkBody(new Long[] {u1, u2, u3}, new String[] {"DEV", "QA"}).toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].roles.length()").value(2));

    org.junit.jupiter.api.Assertions.assertEquals(3, repo.count());
    org.junit.jupiter.api.Assertions.assertEquals(6, roleRepo.count());
  }

  /** TC-PMEMB-002 重复 bulk → merge，不抛错，行数不变. */
  @Test
  void bulk_add_repeated_is_idempotent() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkBody(new Long[] {u1, u2}, new String[] {"DEV", "QA"}).toString()))
        .andExpect(status().isCreated());
    long pmAfter1 = repo.count();
    long roleAfter1 = roleRepo.count();
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkBody(new Long[] {u1, u2}, new String[] {"DEV", "QA"}).toString()))
        .andExpect(status().isCreated());
    org.junit.jupiter.api.Assertions.assertEquals(pmAfter1, repo.count());
    org.junit.jupiter.api.Assertions.assertEquals(roleAfter1, roleRepo.count());
  }

  /** TC-PMEMB-003 owner 被跳过，不抛 400. */
  @Test
  void bulk_add_skips_owner() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    bulkBody(new Long[] {ownerId, u1}, new String[] {"DEV"}).toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.length()").value(1)) // owner skipped
        .andExpect(jsonPath("$[0].userId").value(u1));
    org.junit.jupiter.api.Assertions.assertEquals(1, repo.count());
  }

  /** TC-PMEMB-004 已有成员 + bulk add 追加新 role → merge. */
  @Test
  void bulk_add_merges_existing_member() throws Exception {
    // first: single-add u1 as DEV
    ObjectNode single = json.createObjectNode();
    single.put("userId", u1);
    single.put("role", "DEV");
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(single.toString()))
        .andExpect(status().isCreated());
    long pmAfter1 = repo.count();
    // bulk: u1 + QA (merge → +1 assignment, no new PM)
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkBody(new Long[] {u1}, new String[] {"QA"}).toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$[0].roles.length()").value(2));
    org.junit.jupiter.api.Assertions.assertEquals(pmAfter1, repo.count());
    org.junit.jupiter.api.Assertions.assertEquals(2, roleRepo.count()); // DEV + QA
  }

  /** TC-PMEMB-005 非法 role → 400. */
  @Test
  void bulk_add_invalid_role_400() throws Exception {
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bulkBody(new Long[] {u1}, new String[] {"FAKE"}).toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-PMEMB-006 空 memberUserIds → 400 (validation). */
  @Test
  void bulk_add_empty_userIds_400() throws Exception {
    ObjectNode n = json.createObjectNode();
    n.putArray("memberUserIds");
    n.putArray("projectRoles").add("DEV");
    mockMvc
        .perform(
            post("/api/projects/" + projectId + "/members/bulk")
                .requestAttr(AuthController.ATTR_USERNAME, "lina")
                .contentType(MediaType.APPLICATION_JSON)
                .content(n.toString()))
        .andExpect(status().isBadRequest());
  }
}
