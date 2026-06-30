/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.notification.domain.Notification;
import com.rainier.notification.repository.NotificationRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.risk.RiskFinding;
import com.rainier.risk.RiskService;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * v0.0.72 (A8) — 验证 RiskService.runAll 对每条 CRIT finding 旁路写入站内通知。
 * 使用 BlockedStoryRule 触发 CRIT。
 */
@SpringBootTest
@ActiveProfiles("test")
class RiskServicePushIntegrationTest {

  @Autowired private RiskService riskService;
  @Autowired private NotificationRepository notificationRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private TaskRepository taskRepo;

  private Long aliceId;

  @BeforeEach
  void seed() {
    notificationRepo.deleteAll();
    taskRepo.deleteAll();
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    userRoleRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();

    User alice = new User();
    alice.setLoginName("alice");
    alice.setName("alice");
    alice.setIsInternal(true);
    alice.setEnabled(true);
    alice = userRepo.saveAndFlush(alice);
    aliceId = alice.getId();

    Project p = new Project();
    p.setCode("P1");
    p.setName("P1");
    p.setStatus("ACTIVE");
    p.setOwnerUserId(aliceId);
    p.setEnabled(true);
    p = projectRepo.saveAndFlush(p);

    UserRole ur = new UserRole();
    ur.setUserId(aliceId);
    ur.setRoleId(1L);
    ur.setProjectId(p.getId());
    userRoleRepo.saveAndFlush(ur);

    Requirement r = new Requirement();
    r.setCode("R1");
    r.setTitle("R1");
    r.setStatus("DRAFT");
    r.setPriority("MEDIUM");
    r.setOwnerUserId(aliceId);
    r.setProjectId(p.getId());
    r = requirementRepo.saveAndFlush(r);

    Sprint sp = new Sprint();
    sp.setCode("SP1");
    sp.setName("SP1");
    sp.setStatus(SprintStatus.ACTIVE);
    sp.setRequirementId(r.getId());
    sp.setOwnerUserId(aliceId);
    sp = sprintRepo.saveAndFlush(sp);

    Story blocked = new Story();
    blocked.setCode("S-BLK");
    blocked.setTitle("Blocked story");
    blocked.setStatus(StoryStatus.BLOCKED);
    blocked.setPriority("MEDIUM");
    blocked.setProjectId(p.getId());
    blocked.setOwnerUserId(aliceId);
    blocked.setSprintId(sp.getId());
    storyRepo.saveAndFlush(blocked);
  }

  /** Scenario 4 — runAll 命中 CRIT → notification 表新增 ≥1 行 level=CRIT 且 title 含 "风险". */
  @Test
  void runAll_critFinding_writesNotification() {
    List<RiskFinding> findings = riskService.runAll("alice", "mine");
    boolean anyCrit = false;
    for (RiskFinding f : findings) {
      if (RiskFinding.LEVEL_CRIT.equals(f.getLevel())) {
        anyCrit = true;
        break;
      }
    }
    assertThat(anyCrit).as("expected at least one CRIT finding").isTrue();

    List<Notification> notifs = notificationRepo.findAll();
    boolean foundCrit = false;
    for (Notification n : notifs) {
      if (aliceId.equals(n.getUserId())
          && "CRIT".equals(n.getLevel())
          && n.getTitle() != null
          && n.getTitle().contains("风险")) {
        foundCrit = true;
        break;
      }
    }
    assertThat(foundCrit)
        .as("expected at least one CRIT notification for alice with title containing 风险")
        .isTrue();
  }

  /** TC-RDEDUP-001: 同一 CRIT finding 连续扫描两次，只保留一条未读风险通知。 */
  @Test
  void runAll_sameCritFindingTwice_suppressesDuplicateNotification() {
    riskService.runAll("alice", "mine");
    riskService.runAll("alice", "mine");

    int matching = 0;
    for (Notification n : notificationRepo.findAll()) {
      if (aliceId.equals(n.getUserId())
          && "CRIT".equals(n.getLevel())
          && "STORY".equals(n.getEntityType())
          && n.getBody() != null
          && n.getBody().contains("BlockedStoryRule")) {
        matching++;
      }
    }
    assertThat(matching).isEqualTo(1);
  }
}
