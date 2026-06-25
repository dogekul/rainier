/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.authz;

import com.rainier.auth.controller.AuthController;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

/**
 * v0.0.64 — 权限判定 helper（无 Spring Security，全靠 controller 显式调）。
 *
 * <p>当前用户 = SecurityFilter 注入到 request 的 ATTR_USERNAME 属性（login_name）。
 * adminAccess = 该 user 通过 user_role 关联的任一 role.admin_access=true。
 *
 * <p>本类无写操作，故方法名无须避开 create/update/delete（AuditAspect 不匹配 read 方法）。
 */
@Service
public class AuthzService {

  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final RoleRepository roleRepo;
  private final ProjectRepository projectRepo;

  public AuthzService(
      UserRepository userRepo,
      UserRoleRepository userRoleRepo,
      RoleRepository roleRepo,
      ProjectRepository projectRepo) {
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.roleRepo = roleRepo;
    this.projectRepo = projectRepo;
  }

  /** Returns the current user's id, or null when the request is anonymous / not bound to a User row. */
  public Long currentUserId(HttpServletRequest req) {
    String username = currentUsername(req);
    if (username == null) return null;
    User u = userRepo.findByLoginName(username).orElse(null);
    return u == null ? null : u.getId();
  }

  /** Returns the current user's login_name (or null if anonymous). */
  public String currentUsername(HttpServletRequest req) {
    if (req == null) return null;
    Object v = req.getAttribute(AuthController.ATTR_USERNAME);
    return v == null ? null : v.toString();
  }

  /** True if the current user has any role with admin_access=true. */
  public boolean isCurrentUserAdmin(HttpServletRequest req) {
    Long uid = currentUserId(req);
    return uid != null && isAdmin(uid);
  }

  public boolean isAdmin(Long userId) {
    if (userId == null) return false;
    List<UserRole> ur = userRoleRepo.findByUserId(userId);
    if (ur.isEmpty()) return false;
    Set<Long> roleIds = new HashSet<Long>();
    for (UserRole r : ur) {
      if (r.getRoleId() != null) roleIds.add(r.getRoleId());
    }
    if (roleIds.isEmpty()) return false;
    for (Role r : roleRepo.findAllById(roleIds)) {
      if (Boolean.TRUE.equals(r.getAdminAccess())) return true;
    }
    return false;
  }

  /**
   * v0.0.64 — 项目成员管理权限判定。
   *
   * <p>当前用户必须是项目 owner OR project.pmoUserId OR adminAccess=true。
   */
  public boolean canManageProjectMembers(Long userId, Long projectId) {
    if (userId == null || projectId == null) return false;
    if (isAdmin(userId)) return true;
    Project p = projectRepo.findById(projectId).orElse(null);
    if (p == null) return false;
    if (userId.equals(p.getOwnerUserId())) return true;
    if (userId.equals(p.getPmoUserId())) return true;
    return false;
  }
}
