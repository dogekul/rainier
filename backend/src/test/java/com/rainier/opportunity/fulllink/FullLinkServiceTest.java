/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.fulllink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.common.exception.NotFoundException;
import com.rainier.customer.domain.Customer;
import com.rainier.customer.repository.CustomerRepository;
import com.rainier.operation.domain.Operation;
import com.rainier.operation.domain.OperationStage;
import com.rainier.operation.repository.OperationRepository;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityArtifact;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.domain.StageActivity;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.opportunity.repository.StageActivityRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.94 D6 — FullLinkService 全链聚合 spec. */
@SpringBootTest
@ActiveProfiles("test")
class FullLinkServiceTest {

  @Autowired private FullLinkService service;
  @Autowired private OpportunityRepository oppRepo;
  @Autowired private OpportunityArtifactRepository artifactRepo;
  @Autowired private StageActivityRepository activityRepo;
  @Autowired private OperationRepository operationRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private CustomerRepository customerRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private com.rainier.requirement.repository.RequirementRepository requirementRepo;

  @BeforeEach
  void cleanDb() {
    activityRepo.deleteAll();
    artifactRepo.deleteAll();
    requirementRepo.deleteAll();
    operationRepo.deleteAll();
    oppRepo.deleteAll();
    projectRepo.deleteAll();
    customerRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String name) {
    User u = new User();
    u.setLoginName(name);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedCustomer(String name) {
    Customer c = new Customer();
    c.setName(name);
    c.setIndustry("FINANCE");
    return customerRepo.saveAndFlush(c).getId();
  }

  private Long seedProject(String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code + "-name");
    p.setStatus("ACTIVE");
    p.setOwnerUserId(seedUser("owner-" + code));
    p.setEnabled(true);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    return projectRepo.saveAndFlush(p).getId();
  }

  /** Scenario A — complete chain: opportunity at ACCEPTANCE + linked project + operation + customer. */
  @Test
  void scenarioA_completeChain_returnsAllFields() {
    Long customerId = seedCustomer("Acme");
    Long projectId = seedProject("ED-1");

    Opportunity opp = new Opportunity();
    opp.setCustomerName("Acme");
    opp.setTitle("ERP 升级");
    opp.setStage(OpportunityStage.ACCEPTANCE);
    opp.setStatus(OpportunityStatus.WON);
    opp.setCustomerId(customerId);
    opp.setProjectId(projectId);
    Long oppId = oppRepo.saveAndFlush(opp).getId();

    Operation op = new Operation();
    op.setCustomerName("Acme");
    op.setTitle("Acme 运营");
    op.setStage(OperationStage.MAINTENANCE);
    op.setStatus(Operation.ACTIVE);
    op.setProjectId(projectId);
    op.setOpportunityId(oppId);
    operationRepo.saveAndFlush(op);

    // seed activities at SURVEY (delivery stage) — 2 total, 1 done
    seedActivity(oppId, OpportunityStage.SURVEY, StageActivity.STATUS_DONE);
    seedActivity(oppId, OpportunityStage.SURVEY, StageActivity.STATUS_PENDING);
    // seed activity at LEAD (presale stage)
    seedActivity(oppId, OpportunityStage.LEAD, StageActivity.STATUS_DONE);
    // seed an artifact
    OpportunityArtifact a = new OpportunityArtifact();
    a.setOpportunityId(oppId);
    a.setType("RESEARCH_REPORT");
    a.setTitle("调研报告");
    a.setContent("body");
    artifactRepo.saveAndFlush(a);

    FullLinkResponse out = service.buildFor(oppId);

    assertNotNull(out.getOpportunity());
    assertEquals(oppId, out.getOpportunity().getId());
    assertNotNull(out.getCustomer());
    assertEquals("Acme", out.getCustomer().getName());
    assertNotNull(out.getProject());
    assertEquals(projectId, out.getProject().getId());
    assertNotNull(out.getOperation());
    assertEquals("Acme 运营", out.getOperation().getTitle());

    assertEquals(5, out.getPresaleStages().size());
    assertEquals(5, out.getDeliveryStages().size());
    // LEAD stage activity counts
    StageSummary lead = out.getPresaleStages().get(0);
    assertEquals(OpportunityStage.LEAD, lead.getCode());
    assertEquals("线索", lead.getLabel());
    assertEquals(1, lead.getActivityCount());
    assertEquals(1, lead.getDoneCount());
    assertFalse(lead.isCurrent());

    // ACCEPTANCE is current
    StageSummary acceptance = out.getDeliveryStages().get(4);
    assertEquals(OpportunityStage.ACCEPTANCE, acceptance.getCode());
    assertTrue(acceptance.isCurrent());
    // artifact total surfaces at the current stage
    assertEquals(1, acceptance.getArtifactCount());

    // SURVEY activities
    StageSummary survey = out.getDeliveryStages().get(1);
    assertEquals(OpportunityStage.SURVEY, survey.getCode());
    assertEquals(2, survey.getActivityCount());
    assertEquals(1, survey.getDoneCount());
  }

  /** Scenario B — early chain: LEAD only, no project / operation / customer. */
  @Test
  void scenarioB_earlyChain_nullsForUnbuiltSegments() {
    Opportunity opp = new Opportunity();
    opp.setCustomerName("Beta Co");
    opp.setTitle("初次接触");
    opp.setStage(OpportunityStage.LEAD);
    opp.setStatus(OpportunityStatus.OPEN);
    Long oppId = oppRepo.saveAndFlush(opp).getId();

    FullLinkResponse out = service.buildFor(oppId);

    assertNotNull(out.getOpportunity());
    assertNull(out.getCustomer());
    assertNull(out.getProject());
    assertNull(out.getOperation());
    assertEquals(5, out.getPresaleStages().size());
    assertTrue(out.getPresaleStages().get(0).isCurrent());
    assertEquals(0, out.getPresaleStages().get(0).getActivityCount());
  }

  /** Scenario C — unknown opportunity → NotFound. */
  @Test
  void scenarioC_unknownOpportunity_throws404() {
    assertThrows(NotFoundException.class, () -> service.buildFor(999999L));
  }

  private void seedActivity(Long oppId, String stageCode, String status) {
    StageActivity a = new StageActivity();
    a.setOpportunityId(oppId);
    a.setStageCode(stageCode);
    a.setActivityTitle("act-" + stageCode);
    a.setStatus(status);
    activityRepo.saveAndFlush(a);
  }
}
