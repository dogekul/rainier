/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.83 (C3) GET /api/users/{id}/profile — self + direct-manager access only. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private TaskRepository taskRepo;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
    taskRepo.deleteAll();
    userOrgRepo.deleteAll();
    orgRepo.deleteAll();
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

  private Long seedOrg(String code, OrganizationType type, Long parentId) {
    Organization o = new Organization();
    o.setCode(code);
    o.setName(code);
    o.setType(type);
    o.setParentId(parentId);
    o.setEnabled(true);
    return orgRepo.saveAndFlush(o).getId();
  }

  private void member(Long userId, Long orgId, UserOrgRole role, boolean primary) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(orgId);
    uo.setRole(role);
    uo.setIsPrimary(primary);
    uo.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
    userOrgRepo.saveAndFlush(uo);
  }

  private void seedStory(String code, Long ownerId) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("Story " + code);
    s.setStatus(StoryStatus.READY);
    s.setPriority("MEDIUM");
    s.setSprintId(1L);
    s.setOwnerUserId(ownerId);
    storyRepo.saveAndFlush(s);
  }

  private void seedTask(String code, Long assigneeId) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle("Task " + code);
    t.setProjectId(1L);
    t.setStatus("TODO");
    t.setPriority("MEDIUM");
    t.setAssigneeUserId(assigneeId);
    taskRepo.saveAndFlush(t);
  }

  /** TC-SUBPROF-001: caller == target → 200. */
  @Test
  void selfAccess_returnsProfile() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.MEMBER, true);
    seedStory("S-1", alice);
    seedTask("T-1", alice);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/users/" + alice + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(alice))
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.ownedStoryCount").value(1))
        .andExpect(jsonPath("$.assignedTaskCount").value(1));
  }

  /** TC-SUBPROF-002: caller is HEAD of target's primary team → 200. */
  @Test
  void subordinateAccess_teamHead_returnsProfile() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long team = seedOrg("T1", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.HEAD, true);
    member(bob, team, UserOrgRole.MEMBER, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/users/" + bob + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(bob))
        .andExpect(jsonPath("$.name").value("Bob"))
        // bob's manager should be alice (HEAD of his team)
        .andExpect(jsonPath("$.manager.userId").value(alice));
  }

  /** TC-SUBPROF-003: caller is HEAD of target's primary org's parent → 200. */
  @Test
  void subordinateAccess_parentOrgHead_returnsProfile() throws Exception {
    Long dean = seedUser("dean", "Dean");
    Long bob = seedUser("bob", "Bob");
    Long dept = seedOrg("D", OrganizationType.DEPARTMENT, null);
    Long team = seedOrg("T", OrganizationType.TEAM, dept);
    member(dean, dept, UserOrgRole.HEAD, true);
    member(bob, team, UserOrgRole.MEMBER, true);
    String token = authService.issueToken("dean");

    mockMvc
        .perform(get("/api/users/" + bob + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(bob));
  }

  /** TC-SUBPROF-004: peer (same team MEMBER) → 403. */
  @Test
  void peerAccess_sameTeamMember_returns403() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.MEMBER, true);
    member(bob, team, UserOrgRole.MEMBER, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/users/" + bob + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  /** TC-SUBPROF-005: unrelated user (different team, no manager relation) → 403. */
  @Test
  void unrelatedAccess_differentTeam_returns403() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long charlie = seedUser("charlie", "Charlie");
    Long team1 = seedOrg("T1", OrganizationType.TEAM, null);
    Long team2 = seedOrg("T2", OrganizationType.TEAM, null);
    member(alice, team1, UserOrgRole.HEAD, true);
    member(charlie, team2, UserOrgRole.MEMBER, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(
            get("/api/users/" + charlie + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  /** TC-SUBPROF-006: target user does not exist → 404. */
  @Test
  void targetNotFound_returns404() throws Exception {
    seedUser("alice", "Alice");
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/users/999999/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  /** TC-SUBPROF-007: no token → 401. */
  @Test
  void noToken_returns401() throws Exception {
    Long bob = seedUser("bob", "Bob");
    mockMvc.perform(get("/api/users/" + bob + "/profile")).andExpect(status().isUnauthorized());
  }

  /** TC-SUBPROF-008: token subject has no matching user → 403. */
  @Test
  void ghostToken_returns403() throws Exception {
    Long bob = seedUser("bob", "Bob");
    String token = authService.issueToken("ghost");

    mockMvc
        .perform(get("/api/users/" + bob + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  /** TC-SUBPROF-009: target has no active memberships → only self can read; non-self → 403. */
  @Test
  void targetWithNoMembership_nonSelf_returns403() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long lonely = seedUser("lonely", "Lonely");
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.HEAD, true);
    // lonely is intentionally not added to any org
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/users/" + lonely + "/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
