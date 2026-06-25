/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.controller;

import com.rainier.common.authz.AuthzService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.projectmember.dto.ProjectMemberBulkAddRequest;
import com.rainier.projectmember.dto.ProjectMemberCreateRequest;
import com.rainier.projectmember.dto.ProjectMemberDetail;
import com.rainier.projectmember.dto.ProjectMemberUpdateRequest;
import com.rainier.projectmember.service.ProjectMemberService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.64 — Project member REST 接口.
 *
 * <ul>
 *   <li>GET 任何登录用户
 *   <li>POST/PUT/DELETE owner / project.pmoUserId / adminAccess
 * </ul>
 */
@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectMemberController {

  private final ProjectMemberService service;
  private final AuthzService authz;

  public ProjectMemberController(ProjectMemberService service, AuthzService authz) {
    this.service = service;
    this.authz = authz;
  }

  @GetMapping
  public List<ProjectMemberDetail> list(@PathVariable Long projectId) {
    return service.listMembers(projectId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectMemberDetail add(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectMemberCreateRequest req,
      HttpServletRequest httpReq) {
    requireManagePermission(projectId, httpReq);
    return service.create(projectId, req);
  }

  /** v0.0.88 (C8) — 批量加人 + 多 role（笛卡尔积）. */
  @PostMapping("/bulk")
  @ResponseStatus(HttpStatus.CREATED)
  public List<ProjectMemberDetail> bulkAdd(
      @PathVariable Long projectId,
      @Valid @RequestBody ProjectMemberBulkAddRequest req,
      HttpServletRequest httpReq) {
    requireManagePermission(projectId, httpReq);
    return service.bulkAdd(projectId, req);
  }

  @PutMapping("/{userId}")
  public ProjectMemberDetail updateRole(
      @PathVariable Long projectId,
      @PathVariable Long userId,
      @Valid @RequestBody ProjectMemberUpdateRequest req,
      HttpServletRequest httpReq) {
    requireManagePermission(projectId, httpReq);
    return service.update(projectId, userId, req);
  }

  @DeleteMapping("/{userId}")
  public void remove(
      @PathVariable Long projectId, @PathVariable Long userId, HttpServletRequest httpReq) {
    requireManagePermission(projectId, httpReq);
    service.delete(projectId, userId);
  }

  private void requireManagePermission(Long projectId, HttpServletRequest httpReq) {
    Long currentUserId = authz.currentUserId(httpReq);
    if (!authz.canManageProjectMembers(currentUserId, projectId)) {
      throw new ForbiddenException("无权管理项目成员");
    }
  }
}
