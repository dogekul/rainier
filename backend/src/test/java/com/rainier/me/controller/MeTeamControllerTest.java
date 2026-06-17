/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
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

/** v0.0.24 self-scoped team endpoints. Covers TC-MT-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeTeamControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;

  @BeforeEach
  void cleanDb() {
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

  private Long seedTeam(String code, String name) {
    Organization o = new Organization();
    o.setCode(code);
    o.setName(name);
    o.setType(OrganizationType.TEAM);
    o.setEnabled(true);
    return orgRepo.saveAndFlush(o).getId();
  }

  private void member(Long userId, Long orgId, UserOrgRole role, boolean active) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(orgId);
    uo.setRole(role);
    uo.setIsPrimary(false);
    uo.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
    if (!active) {
      uo.setLeftAt(Instant.parse("2026-03-01T00:00:00Z"));
    }
    userOrgRepo.saveAndFlush(uo);
  }

  /** TC-MT-001: HEAD of one team → led-teams returns it. */
  @Test
  void ledTeams_headOfOneTeam_returnsIt() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long team = seedTeam("T-1", "采购小队");
    member(alice, team, UserOrgRole.HEAD, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/led-teams").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].organizationId").value(team))
        .andExpect(jsonPath("$[0].organizationName").value("采购小队"))
        .andExpect(jsonPath("$[0].organizationType").value("TEAM"));
  }

  /** TC-MT-002: team-members as HEAD returns ACTIVE members, excludes left ones. */
  @Test
  void teamMembers_asHead_returnsActiveMembers() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long carol = seedUser("carol", "Carol");
    Long gone = seedUser("gone", "Gone");
    Long team = seedTeam("T-1", "采购小队");
    member(alice, team, UserOrgRole.HEAD, true);
    member(bob, team, UserOrgRole.MEMBER, true);
    member(carol, team, UserOrgRole.MEMBER, true);
    member(gone, team, UserOrgRole.MEMBER, false); // left → excluded
    String token = authService.issueToken("alice");

    mockMvc
        .perform(
            get("/api/me/team-members")
                .param("organizationId", String.valueOf(team))
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(3)))
        .andExpect(
            jsonPath("$[*].loginName", containsInAnyOrder("alice", "bob", "carol")));
  }

  /** TC-MT-003: a non-HEAD calling team-members for that team → 403 (self-scope re-check). */
  @Test
  void teamMembers_nonHead_returns403() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long team = seedTeam("T-1", "采购小队");
    member(alice, team, UserOrgRole.HEAD, true);
    member(bob, team, UserOrgRole.MEMBER, true);
    String bobToken = authService.issueToken("bob");

    mockMvc
        .perform(
            get("/api/me/team-members")
                .param("organizationId", String.valueOf(team))
                .header("Authorization", "Bearer " + bobToken))
        .andExpect(status().isForbidden());
  }

  /** TC-MT-004: no token → 401. */
  @Test
  void ledTeams_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/led-teams")).andExpect(status().isUnauthorized());
  }

  /** TC-MT-005: HEAD of no team → empty led-teams. */
  @Test
  void ledTeams_headOfNothing_returnsEmpty() throws Exception {
    seedUser("alice", "Alice");
    String token = authService.issueToken("alice");
    mockMvc
        .perform(get("/api/me/led-teams").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  /** TC-MT-006: only HEAD assignments count — a MEMBER's led-teams is empty. */
  @Test
  void ledTeams_memberRole_notCounted() throws Exception {
    Long bob = seedUser("bob", "Bob");
    Long team = seedTeam("T-1", "采购小队");
    member(bob, team, UserOrgRole.MEMBER, true);
    String token = authService.issueToken("bob");
    mockMvc
        .perform(get("/api/me/led-teams").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }
}
