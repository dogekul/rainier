/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.risk.rules.BlockedStoryRule;
import com.rainier.risk.rules.OverdueTaskRule;
import com.rainier.risk.rules.SprintEndingNoDoneRule;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.70 — risk-radar 规则版：3 个规则单测 + RiskService 聚合 + /api/me/risks 端点。 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskRulesIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private UserRoleRepository userRoleRepo;

  @Autowired private OverdueTaskRule overdueTaskRule;
  @Autowired private BlockedStoryRule blockedStoryRule;
  @Autowired private SprintEndingNoDoneRule sprintEndingRule;
  @Autowired private RiskService riskService;

  private Long aliceId;
  private Long p1Id;
  private Long p2Id;
  private Long overdueTaskId;
  private Long blockedStoryId;
  private Long endingSprintId;

  @BeforeEach
  void seed() {
    taskRepo.deleteAll();
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    userRoleRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();

    aliceId = newUser("alice").getId();
    p1Id = newProject("P1", aliceId).getId();
    p2Id = newProject("P2", newUser("bob").getId()).getId();
    grantRoleOnProject(aliceId, p1Id);

    // overdue task in P1 (status=IN_PROGRESS, due yesterday)
    overdueTaskId = newTask("T-OVR", p1Id, TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(1), null).getId();
    // done overdue — must be ignored
    newTask("T-DONE", p1Id, TaskStatus.DONE, LocalDate.now().minusDays(5), null);
    // future task — clean
    newTask("T-FUT", p1Id, TaskStatus.TODO, LocalDate.now().plusDays(7), null);
    // overdue but in P2 (out of scope when scope=[P1])
    newTask("T-OOS", p2Id, TaskStatus.IN_PROGRESS, LocalDate.now().minusDays(1), null);

    // requirements + sprints
    Requirement r1 = newRequirement("R1", p1Id, aliceId);
    Requirement r2 = newRequirement("R2", p2Id, aliceId);
    // sprint in P1 ending in 2 days
    Sprint sp =
        newSprint(
            "SP-END", r1.getId(), aliceId, LocalDate.now().minusDays(5), LocalDate.now().plusDays(2));
    endingSprintId = sp.getId();
    // sprint with one DONE task — should NOT fire SprintEndingNoDoneRule
    Sprint spOk =
        newSprint(
            "SP-OK", r1.getId(), aliceId, LocalDate.now().minusDays(5), LocalDate.now().plusDays(1));
    newTask("T-SPOK", p1Id, TaskStatus.DONE, null, spOk.getId());
    // sprint in scope but ending far in the future — outside the 3-day window
    newSprint(
        "SP-FAR", r1.getId(), aliceId, LocalDate.now(), LocalDate.now().plusDays(30));
    // sprint in P2 — out of scope
    newSprint(
        "SP-OOS", r2.getId(), aliceId, LocalDate.now(), LocalDate.now().plusDays(2));

    // stories
    blockedStoryId = newStory("S-BLK", p1Id, StoryStatus.BLOCKED, aliceId).getId();
    newStory("S-OK", p1Id, StoryStatus.IN_PROGRESS, aliceId);
    newStory("S-OOS", p2Id, StoryStatus.BLOCKED, aliceId); // out of scope
  }

  /** TC-RR-001: OverdueTaskRule only flags non-DONE tasks whose dueDate < today, scope-bounded. */
  @Test
  void overdueTaskRule_findsOverdueOnlyInScope() {
    RiskContext ctx = new RiskContext(aliceId, Collections.singletonList(p1Id), LocalDateTime.now());
    List<RiskFinding> findings = overdueTaskRule.evaluate(ctx);
    assertThat(findings).hasSize(1);
    RiskFinding f = findings.get(0);
    assertThat(f.getLevel()).isEqualTo(RiskFinding.LEVEL_WARN);
    assertThat(f.getEntityType()).isEqualTo("TASK");
    assertThat(f.getEntityId()).isEqualTo(overdueTaskId);
    assertThat(f.getRuleName()).isEqualTo("OverdueTaskRule");
  }

  /** TC-RR-002: BlockedStoryRule only flags BLOCKED stories within scope. */
  @Test
  void blockedStoryRule_findsBlockedOnlyInScope() {
    RiskContext ctx = new RiskContext(aliceId, Collections.singletonList(p1Id), LocalDateTime.now());
    List<RiskFinding> findings = blockedStoryRule.evaluate(ctx);
    assertThat(findings).hasSize(1);
    RiskFinding f = findings.get(0);
    assertThat(f.getLevel()).isEqualTo(RiskFinding.LEVEL_CRIT);
    assertThat(f.getEntityType()).isEqualTo("STORY");
    assertThat(f.getEntityId()).isEqualTo(blockedStoryId);
  }

  /** TC-RR-003: SprintEndingNoDoneRule fires for endDate within next 3 days w/ zero DONE tasks. */
  @Test
  void sprintEndingRule_findsEndingSprintWithoutDone() {
    RiskContext ctx = new RiskContext(aliceId, Collections.singletonList(p1Id), LocalDateTime.now());
    List<RiskFinding> findings = sprintEndingRule.evaluate(ctx);
    assertThat(findings).hasSize(1);
    RiskFinding f = findings.get(0);
    assertThat(f.getLevel()).isEqualTo(RiskFinding.LEVEL_WARN);
    assertThat(f.getEntityType()).isEqualTo("SPRINT");
    assertThat(f.getEntityId()).isEqualTo(endingSprintId);
  }

  /** TC-RR-004: RiskService.runAll fans out across every registered rule. */
  @Test
  void riskService_runAll_aggregatesAllRules() {
    List<RiskFinding> all = riskService.runAll("alice", "mine");
    assertThat(all).extracting(RiskFinding::getRuleName)
        .contains("OverdueTaskRule", "BlockedStoryRule", "SprintEndingNoDoneRule");
    assertThat(all.size()).isGreaterThanOrEqualTo(3);
  }

  /** TC-RR-005: GET /api/me/risks?scope=mine returns the same set as runAll. */
  @Test
  void meRisksEndpoint_returnsAggregatedFindings() throws Exception {
    String token = authService.issueToken("alice");
    mockMvc
        .perform(get("/api/me/risks?scope=mine").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.ruleName=='OverdueTaskRule')].entityType").value(
            org.hamcrest.Matchers.hasItem("TASK")))
        .andExpect(jsonPath("$[?(@.ruleName=='BlockedStoryRule')].entityType").value(
            org.hamcrest.Matchers.hasItem("STORY")))
        .andExpect(jsonPath("$[?(@.ruleName=='SprintEndingNoDoneRule')].entityType").value(
            org.hamcrest.Matchers.hasItem("SPRINT")));
  }

  /** TC-RR-006: no token → 401. */
  @Test
  void meRisksEndpoint_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/me/risks")).andExpect(status().isUnauthorized());
  }

  // ------ helpers ------

  private User newUser(String login) {
    User u = new User();
    u.setLoginName(login);
    u.setName(login);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u);
  }

  private Project newProject(String code, Long ownerId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p);
  }

  private void grantRoleOnProject(Long userId, Long projectId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(1L);
    ur.setProjectId(projectId);
    userRoleRepo.saveAndFlush(ur);
  }

  private Task newTask(String code, Long projectId, String status, LocalDate due, Long sprintId) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle(code);
    t.setProjectId(projectId);
    t.setStatus(status);
    t.setPriority("MEDIUM");
    t.setDueDate(due);
    t.setSprintId(sprintId);
    return taskRepo.saveAndFlush(t);
  }

  private Requirement newRequirement(String code, Long projectId, Long ownerId) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle(code);
    r.setStatus("DRAFT");
    r.setPriority("MEDIUM");
    r.setOwnerUserId(ownerId);
    r.setProjectId(projectId);
    return requirementRepo.saveAndFlush(r);
  }

  private Sprint newSprint(
      String code, Long requirementId, Long ownerId, LocalDate start, LocalDate end) {
    Sprint s = new Sprint();
    s.setCode(code);
    s.setName(code);
    s.setStatus(SprintStatus.ACTIVE);
    s.setRequirementId(requirementId);
    s.setOwnerUserId(ownerId);
    s.setStartDate(start);
    s.setEndDate(end);
    return sprintRepo.saveAndFlush(s);
  }

  private Story newStory(String code, Long projectId, String status, Long ownerId) {
    Story st = new Story();
    st.setCode(code);
    st.setTitle(code);
    st.setStatus(status);
    st.setPriority("MEDIUM");
    st.setSprintId(stubSprintId());
    st.setProjectId(projectId);
    st.setOwnerUserId(ownerId);
    return storyRepo.saveAndFlush(st);
  }

  /** Cheap shared sprint just to satisfy story.sprintId NOT NULL — content irrelevant. */
  private Long _stubSprintId;
  private Long stubSprintId() {
    if (_stubSprintId == null) {
      Requirement stubReq = newRequirement("R-STUB", p1Id, aliceId);
      Sprint stub = newSprint("SP-STUB", stubReq.getId(), aliceId, null, null);
      _stubSprintId = stub.getId();
    }
    return _stubSprintId;
  }

  /** Unused suppression sink for the auto-imports lint. */
  @SuppressWarnings("unused")
  private static final List<String> _PIN = Arrays.asList("a", "b");
}
