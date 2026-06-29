/* (C) 2026 Rainier — internal use only. */
package com.rainier.pmo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.service.AuthService;
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
import com.rainier.userorganization.repository.UserOrganizationRepository;
import com.rainier.userrole.repository.UserRoleRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.110 (H3) — PMO company map endpoint. Covers TC-PMO-01..05. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PmoPortfolioControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepo;
  @Autowired private OrganizationRepository orgRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private MilestoneRepository milestoneRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserOrganizationRepository userOrgRepo;

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

  private Long seedOrg(String code, OrganizationType type) {
    Organization o = new Organization();
    o.setCode(code);
    o.setName(code);
    o.setType(type);
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

  /** TC-PMO-01: groupBy=organization 按组织切片 + RYG 计数 (worst-first). */
  @Test
  void companyMap_groupByOrganization_slicesAndCountsRyg() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    Long orgA = seedOrg("ORG-A", OrganizationType.DEPARTMENT);
    Long orgB = seedOrg("ORG-B", OrganizationType.DEPARTMENT);
    Long p1 = seedProject("P1", alice, orgA);
    Long p2 = seedProject("P2", alice, orgA);
    Long p3 = seedProject("P3", bob, orgB);
    seedTask("t1", p1, "BLOCKED", FUTURE); // RED
    seedTask("t2", p2, "TODO", FUTURE); // GREEN
    seedTask("t3", p3, "TODO", LocalDate.of(2020, 1, 1)); // overdue → likely YELLOW/RED depending on ratio
    String token = authService.issueToken("alice");

    // worst-first: ORG-A has 1 red, ORG-B has 0 red → ORG-A first
    mockMvc
        .perform(
            get("/api/pmo/portfolio?groupBy=organization")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].group.name").value("ORG-A"))
        .andExpect(jsonPath("$[0].group.type").value("DEPARTMENT"))
        .andExpect(jsonPath("$[0].projects.length()").value(2))
        .andExpect(jsonPath("$[0].rygCount.red").value(1))
        .andExpect(jsonPath("$[0].rygCount.green").value(1))
        .andExpect(jsonPath("$[1].group.name").value("ORG-B"))
        .andExpect(jsonPath("$[1].projects.length()").value(1));
  }

  /** TC-PMO-02: groupBy=owner 按 ownerUserId 切片. */
  @Test
  void companyMap_groupByOwner_slicesByOwner() throws Exception {
    Long alice = seedUser("alice");
    Long bob = seedUser("bob");
    seedProject("P1", alice, null);
    seedProject("P2", bob, null);
    seedProject("P3", bob, null);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(
            get("/api/pmo/portfolio?groupBy=owner").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[*].group.name").exists())
        .andExpect(jsonPath("$[*].group.type").value(org.hamcrest.Matchers.hasItem("USER")));
  }

  /** TC-PMO-03: organizationId NULL → 落入 "未归属" group. */
  @Test
  void companyMap_nullOrg_fallsIntoUnassignedGroup() throws Exception {
    Long alice = seedUser("alice");
    seedProject("P-NULL", alice, null);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(
            get("/api/pmo/portfolio?groupBy=organization")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].group.name").value("未归属"))
        .andExpect(jsonPath("$[0].group.id").doesNotExist())
        .andExpect(jsonPath("$[0].projects.length()").value(1));
  }

  /** TC-PMO-04: groupBy=none → 单一 "全公司" group. */
  @Test
  void companyMap_groupByNone_singleAllCompanyGroup() throws Exception {
    Long alice = seedUser("alice");
    seedProject("P1", alice, null);
    seedProject("P2", alice, null);
    String token = authService.issueToken("alice");

    mockMvc
        .perform(
            get("/api/pmo/portfolio?groupBy=none").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].group.name").value("全公司"))
        .andExpect(jsonPath("$[0].projects.length()").value(2));
  }

  /** TC-PMO-05: no token → 401. */
  @Test
  void companyMap_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/pmo/portfolio")).andExpect(status().isUnauthorized());
  }
}
