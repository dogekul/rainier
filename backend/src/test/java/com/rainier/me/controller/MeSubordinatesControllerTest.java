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

/** v0.0.111 (H4) — 我的下属面板入口端点. Covers TC-SUB-001..004. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeSubordinatesControllerTest {

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

  private void member(Long userId, Long orgId, UserOrgRole role, boolean active, boolean primary) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(orgId);
    uo.setRole(role);
    uo.setIsPrimary(primary);
    uo.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
    if (!active) {
      uo.setLeftAt(Instant.parse("2026-03-01T00:00:00Z"));
    }
    userOrgRepo.saveAndFlush(uo);
  }

  /** TC-SUB-001: HEAD returns active members of org (excluding self + left). */
  @Test
  void subordinates_asHead_returnsActiveMembersExcludingSelf() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long carol = seedUser("carol", "Carol");
    Long gone = seedUser("gone", "Gone");
    Long team = seedTeam("T-1", "采购小队");
    member(alice, team, UserOrgRole.HEAD, true, true);
    member(bob, team, UserOrgRole.MEMBER, true, true);
    member(carol, team, UserOrgRole.MEMBER, true, true);
    member(gone, team, UserOrgRole.MEMBER, false, true);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/subordinates").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].loginName", containsInAnyOrder("bob", "carol")))
        .andExpect(jsonPath("$[0].primaryOrgName").value("采购小队"))
        .andExpect(jsonPath("$[0].contributionSummary.weeklyTasksDone").exists())
        .andExpect(jsonPath("$[0].contributionSummary.totalTasks").exists());
  }

  /** TC-SUB-002: non-HEAD caller → empty list (panel is the gate, not 403). */
  @Test
  void subordinates_nonHead_returnsEmpty() throws Exception {
    seedUser("eve", "Eve");
    String token = authService.issueToken("eve");

    mockMvc
        .perform(get("/api/me/subordinates").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  /** TC-SUB-003: no token → 401. */
  @Test
  void subordinates_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/subordinates")).andExpect(status().isUnauthorized());
  }

  /** TC-SUB-004: HEADing multiple orgs aggregates active members across them (de-duped). */
  @Test
  void subordinates_multipleHeadedOrgs_aggregatedAndDedup() throws Exception {
    Long alice = seedUser("alice", "Alice");
    Long bob = seedUser("bob", "Bob");
    Long carol = seedUser("carol", "Carol");
    Long t1 = seedTeam("T-1", "前端组");
    Long t2 = seedTeam("T-2", "后端组");
    member(alice, t1, UserOrgRole.HEAD, true, true);
    member(alice, t2, UserOrgRole.HEAD, true, false);
    member(bob, t1, UserOrgRole.MEMBER, true, true);
    // carol in both teams; should appear once
    member(carol, t1, UserOrgRole.MEMBER, true, true);
    member(carol, t2, UserOrgRole.MEMBER, true, false);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/subordinates").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].loginName", containsInAnyOrder("bob", "carol")));
  }
}
