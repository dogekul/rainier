/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import com.rainier.auditlog.service.AuditLogService;
import com.rainier.authz.PermissionPoint;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.105 G1 — 启动时给所有 {@code adminAccess=true} 的 Role 预绑全部 {@link PermissionPoint}
 * 枚举值。
 *
 * <p>命门：B4 留下的 {@code fine-grained-permissions} flag 之所以一直不敢打开，是因为
 *
 * <ol>
 *   <li>admin role 没有任何 PermissionPoint 预绑，启 flag 后 {@code @RequiresPermission} 端点对 admin 全 403；
 *   <li>{@link AdminRolePermissionController} 自身故意没加 {@code @RequiresPermission(ROLE_MANAGE)}，
 *       因为「首次启用时还没有任何 role 持 ROLE_MANAGE」会死锁。
 * </ol>
 *
 * 本 runner 解决 (1) 并解锁 (2)：admin role 启动后立刻拥有 ROLE_MANAGE，可以安全地给 Controller 补上守卫。
 *
 * <p>幂等：通过 {@link RolePermissionRepository#existsByRoleIdAndPermissionPoint} 跳过已存在条目，
 * 重启不会重复插入。只在「至少新插了一条」时写一条
 * {@code AuditLog action=BOOTSTRAP_ADMIN_PERMISSIONS}。
 *
 * <p>Gated on {@code app.security.admin-permission-bootstrap.enabled}（默认 true，test profile 关闭，
 * 避免污染 870+ 既有测试的 {@code rainier_role_permission} 表）。在 {@link
 * com.rainier.authz.AdminAuthzBootstrap} 之后跑（HIGHEST_PRECEDENCE + 10），确保 admin role 已经存在。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AdminPermissionBootstrap implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminPermissionBootstrap.class);
  private static final String ENTITY_TYPE = "ROLE_PERMISSION";
  // NOTE: AuditLog.action column is length=16; keep this code short. Semantic name preserved in
  // the human-readable summary string.
  private static final String ACTION = "BOOTSTRAP_PERMS";
  private static final String ACTION_NAME = "BOOTSTRAP_ADMIN_PERMISSIONS";

  private final boolean enabled;
  private final RoleRepository roleRepo;
  private final RolePermissionRepository repo;
  private final AuditLogService auditLogService;

  public AdminPermissionBootstrap(
      @Value("${app.security.admin-permission-bootstrap.enabled:true}") boolean enabled,
      RoleRepository roleRepo,
      RolePermissionRepository repo,
      AuditLogService auditLogService) {
    this.enabled = enabled;
    this.roleRepo = roleRepo;
    this.repo = repo;
    this.auditLogService = auditLogService;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) {
      return;
    }
    List<Role> all = roleRepo.findAll();
    List<Long> adminRoleIds = new ArrayList<Long>();
    for (Role r : all) {
      if (Boolean.TRUE.equals(r.getAdminAccess())) {
        adminRoleIds.add(r.getId());
      }
    }
    if (adminRoleIds.isEmpty()) {
      return;
    }
    int totalInserted = 0;
    for (Long roleId : adminRoleIds) {
      for (PermissionPoint pp : PermissionPoint.values()) {
        if (repo.existsByRoleIdAndPermissionPoint(roleId, pp.name())) {
          continue;
        }
        RolePermission rp = new RolePermission();
        rp.setRoleId(roleId);
        rp.setPermissionPoint(pp.name());
        repo.save(rp);
        totalInserted++;
      }
    }
    if (totalInserted > 0) {
      String summary =
          ACTION_NAME
              + " adminRoleIds="
              + adminRoleIds.toString()
              + " inserted="
              + totalInserted
              + " points="
              + PermissionPoint.values().length;
      auditLogService.record("system", ENTITY_TYPE, null, ACTION, summary);
      log.warn(
          "AdminPermissionBootstrap: pre-bound {} permission points across {} admin role(s)",
          totalInserted,
          adminRoleIds.size());
    }
  }
}
