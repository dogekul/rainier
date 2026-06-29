/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import com.rainier.authz.PermissionPoint;
import com.rainier.authz.RequiresPermission;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.role.repository.RoleRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.77 B4 — admin维护角色↔权限点 M2M。Path 在 {@link com.rainier.authz.AdminPaths} Tier A {@code /api/admin}
 * 兜底之下，仍需 admin 身份。
 *
 * <p>v0.0.105 G1 — 写方法补上 {@code @RequiresPermission(ROLE_MANAGE)}：因为
 * {@link AdminPermissionBootstrap} 已在启动时给所有 adminAccess=true 的 role 预绑全部 PermissionPoint，
 * 启用 fine-grained-permissions flag 后 admin 不再自锁，可以安全引入这层守卫。
 * {@code list} 仍只走 AdminPaths 兜底（只读不敏感）。
 */
@RestController
@RequestMapping("/api/admin/roles")
public class AdminRolePermissionController {

  private final RoleRepository roleRepo;
  private final RolePermissionRepository repo;

  public AdminRolePermissionController(RoleRepository roleRepo, RolePermissionRepository repo) {
    this.roleRepo = roleRepo;
    this.repo = repo;
  }

  @GetMapping("/{roleId}/permissions")
  public List<String> list(@PathVariable Long roleId) {
    requireRole(roleId);
    List<RolePermission> rps = repo.findByRoleId(roleId);
    Set<String> uniq = new HashSet<String>();
    for (RolePermission rp : rps) {
      uniq.add(rp.getPermissionPoint());
    }
    List<String> out = new ArrayList<String>(uniq);
    Collections.sort(out);
    return out;
  }

  @PostMapping("/{roleId}/permissions")
  @RequiresPermission(PermissionPoint.ROLE_MANAGE)
  @Transactional
  public Map<String, Object> grant(@PathVariable Long roleId, @RequestBody Map<String, String> body) {
    requireRole(roleId);
    String raw = body == null ? null : body.get("permissionPoint");
    PermissionPoint pp = parse(raw);
    if (repo.existsByRoleIdAndPermissionPoint(roleId, pp.name())) {
      Map<String, Object> ok = new java.util.HashMap<String, Object>();
      ok.put("ok", Boolean.TRUE);
      ok.put("created", Boolean.FALSE);
      return ok;
    }
    RolePermission rp = new RolePermission();
    rp.setRoleId(roleId);
    rp.setPermissionPoint(pp.name());
    repo.save(rp);
    Map<String, Object> ok = new java.util.HashMap<String, Object>();
    ok.put("ok", Boolean.TRUE);
    ok.put("created", Boolean.TRUE);
    return ok;
  }

  @DeleteMapping("/{roleId}/permissions/{permissionPoint}")
  @RequiresPermission(PermissionPoint.ROLE_MANAGE)
  @Transactional
  public Map<String, Object> revoke(
      @PathVariable Long roleId, @PathVariable String permissionPoint) {
    requireRole(roleId);
    PermissionPoint pp = parse(permissionPoint);
    long removed = repo.deleteByRoleIdAndPermissionPoint(roleId, pp.name());
    Map<String, Object> ok = new java.util.HashMap<String, Object>();
    ok.put("ok", Boolean.TRUE);
    ok.put("removed", removed);
    return ok;
  }

  private void requireRole(Long roleId) {
    if (!roleRepo.findById(roleId).isPresent()) {
      throw new NotFoundException("role not found: " + roleId);
    }
  }

  private PermissionPoint parse(String raw) {
    if (raw == null || raw.trim().isEmpty()) {
      throw new BadRequestException("permissionPoint is required");
    }
    try {
      return PermissionPoint.valueOf(raw.trim());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException("unknown permissionPoint: " + raw);
    }
  }
}
