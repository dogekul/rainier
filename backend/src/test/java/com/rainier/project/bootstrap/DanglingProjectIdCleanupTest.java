/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.bootstrap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rainier.common.domain.Priority;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for {@link DanglingProjectIdCleanup}. Covers TC-REQP-004 and TC-URLP-004 — the
 * v0.0.8 startup self-heal that NULLs out any Requirement.project_id / UserRole.project_id values
 * pointing to a non-existent Project.
 */
@SpringBootTest
@ActiveProfiles("test")
class DanglingProjectIdCleanupTest {

  @Autowired private DanglingProjectIdCleanup cleanup;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;

  private ListAppender<ILoggingEvent> logCaptor;
  private Logger cleanupLogger;

  @BeforeEach
  void cleanDb() {
    userRoleRepo.deleteAll();
    requirementRepo.deleteAll();
    userRepo.deleteAll();
    roleRepo.deleteAll();
    // Capture WARN log lines emitted by DanglingProjectIdCleanup to allow per-row assertions.
    cleanupLogger = (Logger) LoggerFactory.getLogger(DanglingProjectIdCleanup.class);
    logCaptor = new ListAppender<>();
    logCaptor.start();
    cleanupLogger.addAppender(logCaptor);
  }

  @AfterEach
  void tearDown() {
    if (cleanupLogger != null && logCaptor != null) {
      cleanupLogger.detachAppender(logCaptor);
      logCaptor.stop();
    }
  }

  /** TC-REQP-004: 启动自愈 — requirement 表 dangling project_id → null. */
  @Test
  void run_cleansDanglingProjectIdFromRequirement() throws Exception {
    Long userId = createUser();
    Requirement r = new Requirement();
    r.setCode("REQ-DANGLE");
    r.setTitle("x");
    r.setDescription("x");
    r.setOwnerUserId(userId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    r.setProjectId(999L); // dangling — no Project id=999 exists
    Long reqId = requirementRepo.saveAndFlush(r).getId();

    cleanup.run();

    Requirement reloaded = requirementRepo.findById(reqId).orElseThrow(IllegalStateException::new);
    assertNull(reloaded.getProjectId(), "expected dangling project_id to be NULL-ed by cleanup");
    // Spec requires per-row WARN log: "cleaned dangling project_id from rainier_requirement.<id>".
    String expected = "cleaned dangling project_id from rainier_requirement." + reqId;
    assertTrue(
        logCaptor.list.stream()
            .anyMatch(
                e ->
                    e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains(expected)
                        && e.getFormattedMessage().contains("was project_id=999")),
        "expected WARN log line matching: " + expected + " (was project_id=999)");
  }

  /** TC-URLP-004: 启动自愈 — user_role 表 dangling project_id → null. */
  @Test
  void run_cleansDanglingProjectIdFromUserRole() throws Exception {
    Long userId = createUser();
    Long roleId = createRole();
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(888L); // dangling — no Project id=888 exists
    Long urId = userRoleRepo.saveAndFlush(ur).getId();

    cleanup.run();

    UserRole reloaded = userRoleRepo.findById(urId).orElseThrow(IllegalStateException::new);
    assertNull(reloaded.getProjectId(), "expected dangling project_id to be NULL-ed by cleanup");
    // Spec requires per-row WARN log: "cleaned dangling project_id from rainier_user_role.<id>".
    String expected = "cleaned dangling project_id from rainier_user_role." + urId;
    assertTrue(
        logCaptor.list.stream()
            .anyMatch(
                e ->
                    e.getLevel() == Level.WARN
                        && e.getFormattedMessage().contains(expected)
                        && e.getFormattedMessage().contains("was project_id=888")),
        "expected WARN log line matching: " + expected + " (was project_id=888)");
  }

  private Long createUser() {
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createRole() {
    Role r = new Role();
    r.setCode("PMO");
    r.setName("PMO");
    r.setEnabled(true);
    return roleRepo.saveAndFlush(r).getId();
  }
}
