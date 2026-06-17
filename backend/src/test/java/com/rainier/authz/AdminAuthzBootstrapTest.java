/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/** v0.0.21: first-admin bootstrap runner. Covers TC-BOOT-001..003 (gate flipped ON). */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.admin-authz.enabled=true")
class AdminAuthzBootstrapTest {

  @Autowired private AdminAuthzBootstrap bootstrap;
  @Autowired private RoleRepository roleRepo;

  @BeforeEach
  void clean() {
    roleRepo.deleteAll();
  }

  private Long seedRole(String code, boolean adminAccess) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    r.setAdminAccess(adminAccess);
    return roleRepo.saveAndFlush(r).getId();
  }

  /** TC-BOOT-001: 无 admin 角色 → 提升 PMO 为 admin. */
  @Test
  void run_noAdmin_elevatesPmo() {
    Long pmoId = seedRole("PMO", false);
    bootstrap.run();
    assertTrue(roleRepo.findById(pmoId).orElseThrow(AssertionError::new).getAdminAccess());
  }

  /** TC-BOOT-002: 已有 admin 角色 → 幂等 no-op（PMO 不被动）. */
  @Test
  void run_adminExists_noop() {
    seedRole("ADMIN", true);
    Long pmoId = seedRole("PMO", false);
    bootstrap.run();
    assertFalse(roleRepo.findById(pmoId).orElseThrow(AssertionError::new).getAdminAccess());
  }

  /** TC-BOOT-003: 无 PMO 角色 → 安全 no-op（不抛异常，无变更）. */
  @Test
  void run_noBootstrapRole_safeNoop() {
    Long otherId = seedRole("DEV", false);
    bootstrap.run();
    assertFalse(roleRepo.findById(otherId).orElseThrow(AssertionError::new).getAdminAccess());
  }
}
