/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves whether a caller is "elevated" (admin). Same source of truth as the v0.0.20 frontend
 * {@code isElevated}: a user is elevated when ANY of their assigned roles has {@code adminAccess=true}
 * ({@link Role#getAdminAccess()} coalesces a legacy NULL to false). Unknown/unauthenticated users are
 * never elevated.
 */
@Service
@Transactional(readOnly = true)
public class ElevationService {

  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final RoleRepository roleRepo;

  public ElevationService(
      UserRepository userRepo, UserRoleRepository userRoleRepo, RoleRepository roleRepo) {
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.roleRepo = roleRepo;
  }

  public boolean isElevated(String username) {
    if (username == null) {
      return false;
    }
    User user = userRepo.findByLoginName(username).orElse(null);
    if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
      // A disabled (but not soft-deleted) account loses elevation immediately, not at token expiry.
      return false;
    }
    List<UserRole> assignments = userRoleRepo.findByUserId(user.getId());
    Set<Long> roleIds =
        assignments.stream()
            .map(UserRole::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (roleIds.isEmpty()) {
      return false;
    }
    for (Role r : roleRepo.findAllById(roleIds)) {
      if (Boolean.TRUE.equals(r.getAdminAccess())) {
        return true;
      }
    }
    return false;
  }
}
