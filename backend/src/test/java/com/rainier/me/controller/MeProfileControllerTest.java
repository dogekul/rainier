/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.position.domain.Position;
import com.rainier.position.repository.PositionRepository;
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

/** v0.0.40 GET /api/me/profile. Covers TC-PROF-001..008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeProfileControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;
  @Autowired private PositionRepository positionRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private TaskRepository taskRepo;

  @BeforeEach
  void cleanDb() {
    storyRepo.deleteAll();
    taskRepo.deleteAll();
    userOrgRepo.deleteAll();
    orgRepo.deleteAll();
    positionRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName, String name, Long positionId) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    u.setPositionId(positionId);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedPosition(String code, String name, String category) {
    Position p = new Position();
    p.setCode(code);
    p.setName(name);
    p.setCategory(category);
    p.setEnabled(true);
    return positionRepo.saveAndFlush(p).getId();
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

  /** TC-PROF-001: identity + position + org membership. */
  @Test
  void profile_identityPositionMembership() throws Exception {
    Long pos = seedPosition("BE", "后端工程师", "TECH");
    Long alice = seedUser("alice", "Alice", pos);
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.MEMBER, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Alice"))
        .andExpect(jsonPath("$.positionName").value("后端工程师"))
        .andExpect(jsonPath("$.positionCategory").value("TECH"))
        .andExpect(jsonPath("$.memberships", hasSize(1)))
        .andExpect(jsonPath("$.memberships[0].organizationId").value(team))
        .andExpect(jsonPath("$.memberships[0].role").value("MEMBER"))
        .andExpect(jsonPath("$.memberships[0].isPrimary").value(true));
  }

  /** TC-PROF-002: manager = active HEAD of the primary org (not self). */
  @Test
  void profile_managerIsTeamHead() throws Exception {
    Long alice = seedUser("alice", "Alice", null);
    Long bob = seedUser("bob", "Bob", null);
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.MEMBER, true);
    member(bob, team, UserOrgRole.HEAD, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.manager.userId").value(bob))
        .andExpect(jsonPath("$.manager.name").value("Bob"));
  }

  /** TC-PROF-003: a team HEAD's manager is the parent-org HEAD (walk up, skip self). */
  @Test
  void profile_headManagerWalksUpToParent() throws Exception {
    Long alice = seedUser("alice", "Alice", null);
    Long carol = seedUser("carol", "Carol", null);
    Long dept = seedOrg("D", OrganizationType.DEPARTMENT, null);
    Long team = seedOrg("T", OrganizationType.TEAM, dept);
    member(alice, team, UserOrgRole.HEAD, true); // alice heads the team
    member(carol, dept, UserOrgRole.HEAD, true); // carol heads the parent dept
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.manager.userId").value(carol))
        .andExpect(jsonPath("$.manager.name").value("Carol"));
  }

  /** TC-PROF-004: no non-self HEAD up the tree → manager null. */
  @Test
  void profile_noManager_null() throws Exception {
    Long alice = seedUser("alice", "Alice", null);
    Long team = seedOrg("T", OrganizationType.TEAM, null);
    member(alice, team, UserOrgRole.HEAD, true); // alice is the only HEAD, no parent
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.manager").doesNotExist());
  }

  /** TC-PROF-005 / TC-PROF-008: contribution counts (soft-deleted excluded via @Where). */
  @Test
  void profile_contributionCounts() throws Exception {
    Long alice = seedUser("alice", "Alice", null);
    seedStory("S-1", alice);
    seedStory("S-2", alice);
    seedStory("S-3", alice);
    seedTask("T-1", alice);
    seedTask("T-2", alice);
    seedTask("T-3", alice);
    seedTask("T-4", alice);
    seedTask("T-5", alice);
    seedTask("T-OTHER", seedUser("bob", "Bob", null)); // not alice's
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ownedStoryCount").value(3))
        .andExpect(jsonPath("$.assignedTaskCount").value(5));
  }

  /** TC-PROF-006: no token → 401. */
  @Test
  void profile_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized());
  }

  /** v0.0.84 (C4): contribution payload — by-status, this-week, 4-week trend. */
  @Test
  void profile_contributionPayload() throws Exception {
    Long alice = seedUser("alice", "Alice", null);
    seedStory("S-1", alice);
    seedTask("T-1", alice);
    seedTask("T-2", alice);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contribution").exists())
        .andExpect(jsonPath("$.contribution.tasksByStatus.TODO").value(2))
        .andExpect(jsonPath("$.contribution.tasksByStatus.DONE").value(0))
        .andExpect(jsonPath("$.contribution.storiesByStatus.READY").value(1))
        .andExpect(jsonPath("$.contribution.weeklyTrend", hasSize(4)));
  }

  /** TC-PROF-007: token subject with no matching User → degraded 200. */
  @Test
  void profile_unknownSubject_degrades() throws Exception {
    String token = authService.issueToken("ghost");
    mockMvc
        .perform(get("/api/me/profile").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.loginName").value("ghost"))
        .andExpect(jsonPath("$.memberships", hasSize(0)))
        .andExpect(jsonPath("$.manager").doesNotExist())
        .andExpect(jsonPath("$.ownedStoryCount").value(0))
        .andExpect(jsonPath("$.assignedTaskCount").value(0));
  }
}
