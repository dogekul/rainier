/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.authz.PermissionPoint;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.77 B4 — PermissionService 解析 user → role → role_permission 的快/慢路径覆盖. */
@SpringBootTest
@ActiveProfiles("test")
class PermissionServiceTest {

  @Autowired private PermissionService permissionService;
  @Autowired private UserRepository userRepo;
  @Autowired private RoleRepository roleRepo;
  @Autowired private UserRoleRepository userRoleRepo;
  @Autowired private RolePermissionRepository rolePermRepo;

  private Long alice;
  private Long bob;
  private Long disabledCarol;
  private Long auditorRole;
  private Long plainRole;

  @BeforeEach
  void seed() {
    rolePermRepo.deleteAll();
    userRoleRepo.deleteAll();
    userRoleRepo.flush();
    roleRepo.deleteAll();
    userRepo.deleteAll();

    alice = seedUser("alice-pt", true);
    bob = seedUser("bob-pt", true);
    disabledCarol = seedUser("carol-pt", false);

    auditorRole = seedRole("AUDITOR");
    plainRole = seedRole("DEV-pt");

    grant(auditorRole, PermissionPoint.AUDIT_VIEW);
    grant(auditorRole, PermissionPoint.COMPLIANCE_VIEW);

    link(alice, auditorRole);
    link(bob, plainRole);
    link(disabledCarol, auditorRole); // even though linked, disabled → no points
  }

  @Test
  void user_withRoleGrantingPoint_hasPermission_true() {
    assertThat(permissionService.hasPermission(alice, PermissionPoint.AUDIT_VIEW)).isTrue();
    assertThat(permissionService.hasPermission(alice, PermissionPoint.COMPLIANCE_VIEW)).isTrue();
  }

  @Test
  void user_withRoleNotGrantingPoint_hasPermission_false() {
    assertThat(permissionService.hasPermission(alice, PermissionPoint.USER_MANAGE)).isFalse();
  }

  @Test
  void user_withNoMatchingRole_hasPermission_false() {
    assertThat(permissionService.hasPermission(bob, PermissionPoint.AUDIT_VIEW)).isFalse();
  }

  @Test
  void disabledUser_hasPermission_false() {
    assertThat(permissionService.hasPermission(disabledCarol, PermissionPoint.AUDIT_VIEW))
        .isFalse();
  }

  @Test
  void nullInputs_returnFalse() {
    assertThat(permissionService.hasPermission(null, PermissionPoint.AUDIT_VIEW)).isFalse();
    assertThat(permissionService.hasPermission(alice, null)).isFalse();
  }

  @Test
  void byUsername_resolvesViaLoginName() {
    assertThat(permissionService.hasPermissionByUsername("alice-pt", PermissionPoint.AUDIT_VIEW))
        .isTrue();
    assertThat(permissionService.hasPermissionByUsername("bob-pt", PermissionPoint.AUDIT_VIEW))
        .isFalse();
    assertThat(permissionService.hasPermissionByUsername("nobody", PermissionPoint.AUDIT_VIEW))
        .isFalse();
  }

  @Test
  void pointsOf_returnsFullSet() {
    assertThat(permissionService.pointsOf(alice))
        .containsExactlyInAnyOrder(PermissionPoint.AUDIT_VIEW, PermissionPoint.COMPLIANCE_VIEW);
    assertThat(permissionService.pointsOf(bob)).isEmpty();
  }

  @Test
  void staleStringConstant_ignoredSilently() {
    RolePermission rp = new RolePermission();
    rp.setRoleId(auditorRole);
    rp.setPermissionPoint("GHOST_RENAMED_POINT");
    rolePermRepo.saveAndFlush(rp);
    // Still returns the valid points, ignoring the stale row.
    assertThat(permissionService.pointsOf(alice))
        .containsExactlyInAnyOrder(PermissionPoint.AUDIT_VIEW, PermissionPoint.COMPLIANCE_VIEW);
  }

  private Long seedUser(String login, boolean enabled) {
    User u = new User();
    u.setLoginName(login);
    u.setName(login);
    u.setIsInternal(true);
    u.setEnabled(enabled);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedRole(String code) {
    Role r = new Role();
    r.setCode(code);
    r.setName(code);
    r.setEnabled(true);
    r.setAdminAccess(false);
    return roleRepo.saveAndFlush(r).getId();
  }

  private void grant(Long roleId, PermissionPoint pp) {
    RolePermission rp = new RolePermission();
    rp.setRoleId(roleId);
    rp.setPermissionPoint(pp.name());
    rolePermRepo.saveAndFlush(rp);
  }

  private void link(Long userId, Long roleId) {
    UserRole ur = new UserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    ur.setProjectId(null);
    userRoleRepo.saveAndFlush(ur);
  }
}
