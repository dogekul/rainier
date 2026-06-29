/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.authz.PermissionPoint;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * v0.0.105 G1 — {@link AdminPermissionBootstrap} happy/idempotent/non-admin-skip + audit row check.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.admin-permission-bootstrap.enabled=true")
class AdminPermissionBootstrapTest {

  @Autowired private AdminPermissionBootstrap bootstrap;
  @Autowired private RoleRepository roleRepo;
  @Autowired private RolePermissionRepository rolePermRepo;
  @Autowired private AuditLogRepository auditLogRepo;

  @BeforeEach
  void clean() {
    rolePermRepo.deleteAll();
    rolePermRepo.flush();
    roleRepo.deleteAll();
    auditLogRepo.deleteAll();
  }

  private Long seedRole(String code, boolean adminAccess) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    r.setAdminAccess(adminAccess);
    return roleRepo.saveAndFlush(r).getId();
  }

  /** Scenario 1: admin role 启动后被预绑全部 PermissionPoint 并写一条审计. */
  @Test
  void run_adminRole_preBindsAllPermissionPoints() {
    Long pmoId = seedRole("PMO", true);

    bootstrap.run();

    List<RolePermission> bound = rolePermRepo.findByRoleId(pmoId);
    Set<String> names = new HashSet<String>();
    for (RolePermission rp : bound) {
      names.add(rp.getPermissionPoint());
    }
    for (PermissionPoint pp : PermissionPoint.values()) {
      assertTrue(names.contains(pp.name()), "missing pre-bind for " + pp.name());
    }
    assertEquals(PermissionPoint.values().length, names.size());
    assertEquals(1, auditLogRepo.findAll().size());
  }

  /** Scenario 2: 重启幂等 — 不重复插入、不再写 audit row. */
  @Test
  void run_idempotent_noDuplicateInserts() {
    Long pmoId = seedRole("PMO", true);
    bootstrap.run();
    long firstCount = rolePermRepo.findByRoleId(pmoId).size();
    long firstAudit = auditLogRepo.findAll().size();

    bootstrap.run();

    assertEquals(firstCount, rolePermRepo.findByRoleId(pmoId).size());
    assertEquals(firstAudit, auditLogRepo.findAll().size());
  }

  /** Scenario 3: 非 admin role 不被绑权限. */
  @Test
  void run_nonAdminRole_skipped() {
    Long devId = seedRole("DEV", false);
    Long pmoId = seedRole("PMO", true);

    bootstrap.run();

    assertTrue(rolePermRepo.findByRoleId(devId).isEmpty());
    assertFalse(rolePermRepo.findByRoleId(pmoId).isEmpty());
  }

  /** Scenario 1 ext: 全部 admin role 都被覆盖. */
  @Test
  void run_multipleAdminRoles_allCovered() {
    Long pmoId = seedRole("PMO", true);
    Long superAdminId = seedRole("SUPER", true);

    bootstrap.run();

    assertEquals(PermissionPoint.values().length, rolePermRepo.findByRoleId(pmoId).size());
    assertEquals(PermissionPoint.values().length, rolePermRepo.findByRoleId(superAdminId).size());
  }
}
