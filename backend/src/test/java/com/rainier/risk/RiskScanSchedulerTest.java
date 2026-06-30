/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** v0.0.117 — risk scan scheduler: periodic scan fans out to enabled users only. */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
    properties = {
      "app.risk.scan.enabled=true",
      "app.risk.scan.scope=mine",
      "app.risk.scan.initial-delay-ms=600000",
      "app.risk.scan.fixed-delay-ms=600000"
    })
class RiskScanSchedulerTest {

  @Autowired private RiskScanScheduler scheduler;
  @Autowired private UserRepository userRepo;

  @MockBean private RiskService riskService;

  @BeforeEach
  void clean() {
    userRepo.deleteAll();
  }

  /** TC-RSCHED-001: scheduled scan runs for enabled users and skips disabled users. */
  @Test
  void scanAllUsers_runsRiskServiceForEnabledUsersOnly() {
    userRepo.saveAndFlush(user("alice", true));
    userRepo.saveAndFlush(user("bob", false));

    scheduler.scanAllUsers();

    verify(riskService).runAll("alice", "mine");
    verify(riskService, never()).runAll("bob", "mine");
  }

  private User user(String loginName, boolean enabled) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(enabled);
    return u;
  }
}
