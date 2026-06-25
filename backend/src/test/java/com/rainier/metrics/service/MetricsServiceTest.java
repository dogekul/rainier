/* (C) 2026 Rainier — internal use only. */
package com.rainier.metrics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.rainier.metrics.dto.CrmMetrics;
import com.rainier.metrics.dto.OverdueProjectRow;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.93 (D5) — MetricsService aggregation. Covers MET-001..005. */
@SpringBootTest
@ActiveProfiles("test")
class MetricsServiceTest {

  @Autowired private MetricsService service;
  @Autowired private OpportunityRepository oppRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  private Instant start;
  private Instant end;

  @BeforeEach
  void clean() {
    oppRepo.deleteAll();
    projectRepo.deleteAll();
    end = Instant.now().plus(1, ChronoUnit.DAYS);
    start = end.minus(30, ChronoUnit.DAYS);
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private void seedOpp(String status) {
    Opportunity o = new Opportunity();
    o.setCustomerName("X 集团");
    o.setTitle("采购系统");
    o.setStage(OpportunityStage.LEAD);
    o.setStatus(status);
    oppRepo.saveAndFlush(o);
  }

  private Long seedProject(
      String code, String status, LocalDate startDate, LocalDate endDate, Long ownerUserId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus(status);
    p.setOwnerUserId(ownerUserId);
    p.setEnabled(true);
    p.setStartDate(startDate);
    p.setEndDate(endDate);
    return projectRepo.saveAndFlush(p).getId();
  }

  /** MET-001 — winRate = WON / (WON+LOST). */
  @Test
  void winRate_threeWonOneLost_returns0_75() {
    seedOpp(OpportunityStatus.WON);
    seedOpp(OpportunityStatus.WON);
    seedOpp(OpportunityStatus.WON);
    seedOpp(OpportunityStatus.LOST);
    seedOpp(OpportunityStatus.OPEN);
    seedOpp(OpportunityStatus.OPEN);

    Double rate = service.winRate(start, end, null);
    assertThat(rate).isNotNull().isCloseTo(0.75, within(1e-6));
  }

  /** MET-002 — dealRate = WON / total. */
  @Test
  void dealRate_twoOfSix_returnsOneThird() {
    seedOpp(OpportunityStatus.WON);
    seedOpp(OpportunityStatus.WON);
    seedOpp(OpportunityStatus.LOST);
    seedOpp(OpportunityStatus.OPEN);
    seedOpp(OpportunityStatus.OPEN);
    seedOpp(OpportunityStatus.OPEN);

    Double rate = service.dealRate(start, end, null);
    assertThat(rate).isNotNull().isCloseTo(2.0 / 6.0, within(1e-6));
  }

  /** MET-003 — winRate null when only OPEN deals exist. */
  @Test
  void winRate_onlyOpen_returnsNull() {
    seedOpp(OpportunityStatus.OPEN);
    seedOpp(OpportunityStatus.OPEN);
    assertThat(service.winRate(start, end, null)).isNull();
  }

  /** MET-004 — avgDeliveryCycleDays averages (end-start) of DELIVERED projects in period. */
  @Test
  void avgDeliveryCycleDays_twoProjects_returns15() {
    Long owner = seedUser("metrics-owner-1");
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    // Project A: 10 days, endDate within period (today).
    seedProject(
        "MET-A", ProjectStatus.DELIVERED, today.minusDays(10), today, owner);
    // Project B: 20 days, endDate within period (yesterday).
    seedProject(
        "MET-B",
        ProjectStatus.DELIVERED,
        today.minusDays(21),
        today.minusDays(1),
        owner);

    Double avg = service.avgDeliveryCycleDays(start, end, null);
    assertThat(avg).isNotNull().isCloseTo(15.0, within(1e-6));
  }

  /** MET-005 — overdueProjects returns only past-due active projects. */
  @Test
  void overdueProjects_excludesFutureAndDelivered() {
    Long owner = seedUser("metrics-owner-2");
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    Long aId =
        seedProject(
            "OVR-A",
            ProjectStatus.ACTIVE,
            today.minusDays(10),
            today.minusDays(1),
            owner); // overdue
    seedProject(
        "OVR-B", ProjectStatus.ACTIVE, today.minusDays(5), today.plusDays(5), owner); // future
    seedProject(
        "OVR-C",
        ProjectStatus.DELIVERED,
        today.minusDays(10),
        today.minusDays(1),
        owner); // delivered

    List<OverdueProjectRow> rows = service.overdueProjects(null);
    assertThat(rows).extracting(OverdueProjectRow::getProjectId).containsExactly(aId);
    assertThat(rows.get(0).getDaysOverdue()).isEqualTo(1L);
  }

  /** Composite snapshot wiring. */
  @Test
  void crmSnapshot_populatesAllFields() {
    seedOpp(OpportunityStatus.WON);
    seedOpp(OpportunityStatus.LOST);
    Long owner = seedUser("metrics-owner-3");
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    seedProject(
        "SNAP-A", ProjectStatus.DELIVERED, today.minusDays(10), today, owner);
    seedProject(
        "SNAP-B", ProjectStatus.ACTIVE, today.minusDays(10), today.minusDays(2), owner);

    CrmMetrics m = service.crmSnapshot(start, end, null);
    assertThat(m.getWinRate()).isNotNull().isCloseTo(0.5, within(1e-6));
    assertThat(m.getDealRate()).isNotNull().isCloseTo(0.5, within(1e-6));
    assertThat(m.getAvgDeliveryCycleDays()).isNotNull().isCloseTo(10.0, within(1e-6));
    assertThat(m.getOverdueProjects()).hasSize(1);
  }
}
