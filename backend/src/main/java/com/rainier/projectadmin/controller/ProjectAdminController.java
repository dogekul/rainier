/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectadmin.controller;

import com.rainier.common.authz.AuthzService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.projectadmin.service.ProjectAdminService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.78 (B5) — 项目级 admin 授权接口。只有全局 admin 才能 grant/revoke 项目管理员。
 *
 * <p>路径 /api/projects/{id}/admins/{userId} —— 故意挂在 /api/projects 下而不是 /api/admin，避免被
 * AdminPaths Tier A "/api/admin" 误捕；自己用 AuthzService.isCurrentUserAdmin 显式判。
 */
@RestController
@RequestMapping("/api/projects/{projectId}/admins")
public class ProjectAdminController {

  private final ProjectAdminService service;
  private final AuthzService authz;

  public ProjectAdminController(ProjectAdminService service, AuthzService authz) {
    this.service = service;
    this.authz = authz;
  }

  @PostMapping("/{userId}")
  public List<Long> grant(
      @PathVariable Long projectId, @PathVariable Long userId, HttpServletRequest req) {
    requireGlobalAdmin(req);
    String by = authz.currentUsername(req);
    return service.updateGrant(projectId, userId, by);
  }

  @DeleteMapping("/{userId}")
  public List<Long> revoke(
      @PathVariable Long projectId, @PathVariable Long userId, HttpServletRequest req) {
    requireGlobalAdmin(req);
    return service.updateRevoke(projectId, userId);
  }

  private void requireGlobalAdmin(HttpServletRequest req) {
    if (!authz.isCurrentUserAdmin(req)) {
      throw new ForbiddenException("仅全局管理员可管理项目级 admin");
    }
  }
}
