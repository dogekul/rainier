/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.milestone.domain.Milestone;
import com.rainier.milestone.repository.MilestoneRepository;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.28 scoped portfolio endpoint. Covers TC-PF-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PortfolioControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private MilestoneRepository milestoneRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;

  private static final LocalDate PAST = LocalDate.of(2020, 1, 1);
  private static final LocalDate FUTURE = LocalDate.of(2099, 12, 31);

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
    milestoneRepo.deleteAll();
    userRoleRepo.deleteAll();
    userOrgRepo.deleteAll();
    projectRepo.deleteAll();
    orgRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
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

  private Long seedProject(String code, Long ownerId, Long orgId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(ownerId);
    p.setOrganizationId(orgId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private void seedTask(String code, Long projectId, String statusVal, LocalDate due) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle(code);
    t.setProjectId(projectId);
    t.setStatus(statusVal);
    t.setPriority("MEDIUM");
    t.setDueDate(due);
    taskRepo.saveAndFlush(t);
  }

  private void headOf(Long userId, Long orgId) {
    UserOrganization uo = new UserOrganization();
    uo.setUserId(userId);
    uo.setOrganizationId(orgId);
    uo.setRole(UserOrgRole.HEAD);
    uo.setIsPrimary(false);
    uo.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
    userOrgRepo.saveAndFlush(uo);
  }

  private void roleOnProject(Long userId, Long projectId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(1L);
    ur.setProjectId(projectId);
    userRoleRepo.saveAndFlush(ur);
  }

  /** TC-PF-001: scope=mine returns projects the user has a role on or owns, with correct RYG. */
  @Test
  void portfolioMine_returnsOwnedAndRoledProjects() throws Exception {
    Long alice = seedUser("alice");
    Long owned = seedProject("OWN", alice, null);
    Long roled = seedProject("ROLE", seedUser("bob"), null);
    roleOnProject(alice, roled);
    seedProject("OTHER", seedUser("carol"), null); // not mine
    seedTask("t1", owned, "TODO", FUTURE); // open, not overdue → GREEN
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/portfolio?scope=mine").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(jsonPath("$[*].projectCode", containsInAnyOrder("OWN", "ROLE")));
  }

  /** TC-PF-002: scope=led descends the org subtree (HEAD of dept covers team-tagged projects). */
  @Test
  void portfolioLed_descendsOrgSubtree() throws Exception {
    Long alice = seedUser("alice");
    Long dept = seedOrg("DEPT", OrganizationType.DEPARTMENT, null);
    Long team = seedOrg("TEAM", OrganizationType.TEAM, dept); // child of dept
    headOf(alice, dept);
    Long pTeam = seedProject("P-TEAM", seedUser("bob"), team); // tagged to child team
    Long pDept = seedProject("P-DEPT", seedUser("carol"), dept); // tagged to dept itself
    seedProject("P-OUT", seedUser("dave"), null); // untagged → excluded
    seedTask("b1", pTeam, "BLOCKED", FUTURE); // blocked → RED
    seedTask("d1", pDept, "TODO", FUTURE); // open, clean → GREEN
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/portfolio?scope=led").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        // sorted worst-first: the blocked (RED) team project leads
        .andExpect(jsonPath("$[*].projectCode", contains("P-TEAM", "P-DEPT")))
        .andExpect(jsonPath("$[0].ryg").value("RED"))
        .andExpect(jsonPath("$[0].blockedTasks").value(1));
  }

  /** TC-PF-003: RYG + overdue counting (overdue ratio drives RED/YELLOW). */
  @Test
  void portfolioMine_countsOverdueAndRyg() throws Exception {
    Long alice = seedUser("alice");
    Long red = seedProject("RED", alice, null);
    // 4 overdue + 6 fresh open → ratio 0.4 → RED
    for (int i = 0; i < 4; i++) seedTask("o" + i, red, "TODO", PAST);
    for (int i = 0; i < 6; i++) seedTask("f" + i, red, "IN_PROGRESS", FUTURE);
    seedTask("done", red, "DONE", PAST); // closed → not counted open/overdue
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/portfolio?scope=mine").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].projectCode").value("RED"))
        .andExpect(jsonPath("$[0].openTasks").value(10))
        .andExpect(jsonPath("$[0].overdueTasks").value(4))
        .andExpect(jsonPath("$[0].ryg").value("RED"));
  }

  /** TC-PF-004: scope=all returns every project + counts overdue milestones. */
  @Test
  void portfolioAll_returnsEveryProject() throws Exception {
    Long alice = seedUser("alice");
    Long p1 = seedProject("P1", alice, null);
    seedProject("P2", seedUser("bob"), null);
    Milestone m = new Milestone();
    m.setProjectId(p1);
    m.setCode("M1");
    m.setName("M1");
    m.setTargetDate(PAST);
    m.setStatus("PLANNED"); // overdue (not REACHED)
    m.setSortOrder(0);
    milestoneRepo.saveAndFlush(m);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(get("/api/me/portfolio?scope=all").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)))
        .andExpect(
            jsonPath("$[?(@.projectCode=='P1')].overdueMilestones").value(contains(1)));
  }

  /** TC-PF-005: default scope is "mine". */
  @Test
  void portfolio_defaultScopeIsMine() throws Exception {
    Long alice = seedUser("alice");
    seedProject("MINE", alice, null);
    seedProject("NOTMINE", seedUser("bob"), null);
    String token = authService.issueToken("alice");
    mockMvc
        .perform(get("/api/me/portfolio").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].projectCode").value("MINE"));
  }

  /** TC-PF-006: no token → 401. */
  @Test
  void portfolio_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/portfolio")).andExpect(status().isUnauthorized());
  }

  /**
   * TC-PF-FP-001 (v0.0.109 H2): scope=footprint surfaces UNTAGGED projects owned by org-subtree
   * members — the whole point: legacy projects without {@code organizationId} are now visible to
   * the team lead.
   */
  @Test
  void portfolioFootprint_includesUntaggedMemberOwnedProjects() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    Long dept = seedOrg("DEPT", OrganizationType.DEPARTMENT, null);
    Long team = seedOrg("TEAM", OrganizationType.TEAM, dept);
    headOf(alice, dept);
    UserOrganization bobUo = new UserOrganization();
    bobUo.setUserId(bob);
    bobUo.setOrganizationId(team);
    bobUo.setRole(UserOrgRole.MEMBER);
    bobUo.setIsPrimary(false);
    bobUo.setJoinedAt(Instant.parse("2026-01-01T00:00:00Z"));
    userOrgRepo.saveAndFlush(bobUo);
    seedProject("P-BOB", bob, null); // untagged → led would miss; footprint catches
    seedProject("P-OUTSIDER", seedUser("dave"), null); // outside subtree → excluded
    String token = authService.issueToken("alice");

    mockMvc
        .perform(
            get("/api/me/portfolio?scope=footprint").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].projectCode").value("P-BOB"));
  }
}
