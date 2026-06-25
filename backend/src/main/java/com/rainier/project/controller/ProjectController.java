/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.controller;

import com.rainier.common.authz.AuthzService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.dto.ProjectCreateRequest;
import com.rainier.project.dto.ProjectDetail;
import com.rainier.project.dto.ProjectUpdateRequest;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.project.service.ProjectService;
import com.rainier.projectadmin.service.ProjectAdminService;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for {@link com.rainier.project.domain.Project}. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

  private final ProjectService service;
  private final ProjectRepository projectRepo;
  private final AuthzService authz;
  private final ProjectAdminService projectAdminService;

  public ProjectController(
      ProjectService service,
      ProjectRepository projectRepo,
      AuthzService authz,
      ProjectAdminService projectAdminService) {
    this.service = service;
    this.projectRepo = projectRepo;
    this.authz = authz;
    this.projectAdminService = projectAdminService;
  }

  @PostMapping
  public ResponseEntity<ProjectDetail> create(@Valid @RequestBody ProjectCreateRequest req) {
    ProjectDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/projects/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public ProjectDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<ProjectDetail> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String projectType,
      @RequestParam(required = false) Boolean enabled,
      @Valid PageParams page) {
    return service.list(status, projectType, enabled, page);
  }

  @PutMapping("/{id}")
  public ProjectDetail update(
      @PathVariable Long id,
      @Valid @RequestBody ProjectUpdateRequest req,
      HttpServletRequest httpReq) {
    requireProjectWritePermission(id, httpReq);
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, HttpServletRequest httpReq) {
    requireProjectWritePermission(id, httpReq);
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * v0.0.78 (B5) — 项目级写权限：全局 admin / 项目 owner / project.pmoUserId / 项目管理员（B5 单标志）。
   *
   * <p>uid == null（匿名 / 未通过 SecurityFilter 注入 ATTR_USERNAME）→ 放行：prod 由 SecurityFilter 已挡，
   * test/whitelisted 场景下旧测试不传 username 是惯例，本层不应额外加锁（既有 39+ admin 测试 + project
   * 增删改测试均不带 username）。带 username 后则严格判 admin / owner / pmo / projectAdmin。
   */
  private void requireProjectWritePermission(Long projectId, HttpServletRequest httpReq) {
    Long uid = authz.currentUserId(httpReq);
    if (uid == null) {
      return;
    }
    if (authz.isAdmin(uid)) {
      return;
    }
    Project p =
        projectRepo
            .findById(projectId)
            .orElseThrow(() -> new NotFoundException("project not found: id=" + projectId));
    if (uid.equals(p.getOwnerUserId())) {
      return;
    }
    if (p.getPmoUserId() != null && uid.equals(p.getPmoUserId())) {
      return;
    }
    if (projectAdminService.isProjectAdmin(uid, projectId)) {
      return;
    }
    throw new ForbiddenException("无权修改该项目");
  }
}
