/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import com.rainier.authz.PermissionPoint;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.77 B4 — resolve fine-grained permission points held by a user via user → role → role_permission.
 *
 * <p>Disabled / unknown / unauthenticated users hold no permissions. ADMIN-by-role-flag
 * (see {@link com.rainier.authz.ElevationService#isElevated}) is INTENTIONALLY NOT a shortcut here:
 * an elevated account still needs the specific point granted to its role(s).
 */
@Service
@Transactional(readOnly = true)
public class PermissionService {

  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final RolePermissionRepository rolePermissionRepo;

  public PermissionService(
      UserRepository userRepo,
      UserRoleRepository userRoleRepo,
      RolePermissionRepository rolePermissionRepo) {
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.rolePermissionRepo = rolePermissionRepo;
  }

  public boolean hasPermission(Long userId, PermissionPoint point) {
    if (userId == null || point == null) {
      return false;
    }
    Set<PermissionPoint> all = pointsOf(userId);
    return all.contains(point);
  }

  public boolean hasPermissionByUsername(String username, PermissionPoint point) {
    Long id = resolveUserId(username);
    return id != null && hasPermission(id, point);
  }

  /** Returns the full point set granted to a user (empty if none / unknown / disabled). */
  public Set<PermissionPoint> pointsOf(Long userId) {
    if (userId == null) {
      return Collections.emptySet();
    }
    User user = userRepo.findById(userId).orElse(null);
    if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
      return Collections.emptySet();
    }
    List<UserRole> assignments = userRoleRepo.findByUserId(userId);
    Set<Long> roleIds =
        assignments.stream()
            .map(UserRole::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (roleIds.isEmpty()) {
      return Collections.emptySet();
    }
    List<RolePermission> rps = rolePermissionRepo.findByRoleIdIn(roleIds);
    Set<PermissionPoint> out = new HashSet<PermissionPoint>();
    for (RolePermission rp : rps) {
      try {
        out.add(PermissionPoint.valueOf(rp.getPermissionPoint()));
      } catch (IllegalArgumentException ignored) {
        // Stale row referencing a renamed/removed constant — skip silently; admin tooling can prune.
      }
    }
    return out;
  }

  public Set<PermissionPoint> pointsOfUsername(String username) {
    Long id = resolveUserId(username);
    return id == null ? Collections.<PermissionPoint>emptySet() : pointsOf(id);
  }

  private Long resolveUserId(String username) {
    if (username == null) {
      return null;
    }
    User u = userRepo.findByLoginName(username).orElse(null);
    return u == null ? null : u.getId();
  }
}
