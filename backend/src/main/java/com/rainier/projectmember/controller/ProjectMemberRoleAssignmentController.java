/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.controller;

import com.rainier.common.authz.AuthzService;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.projectmember.dto.ProjectMemberRoleAddRequest;
import com.rainier.projectmember.dto.ProjectMemberRoleAssignmentDetail;
import com.rainier.projectmember.service.ProjectMemberService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.88 (C8) — ProjectMember ↔ project role 多对多操作端点.
 *
 * <ul>
 *   <li>GET /api/project-members/{id}/roles — 任何登录用户
 *   <li>POST /api/project-members/{id}/roles — canManageProjectMembers
 *   <li>DELETE /api/project-members/{id}/roles/{role} — canManageProjectMembers
 * </ul>
 */
@RestController
@RequestMapping("/api/project-members/{memberId}/roles")
public class ProjectMemberRoleAssignmentController {

  private final ProjectMemberService service;
  private final AuthzService authz;

  public ProjectMemberRoleAssignmentController(ProjectMemberService service, AuthzService authz) {
    this.service = service;
    this.authz = authz;
  }

  @GetMapping
  public List<ProjectMemberRoleAssignmentDetail> list(@PathVariable Long memberId) {
    return service.listRolesOfMember(memberId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectMemberRoleAssignmentDetail add(
      @PathVariable Long memberId,
      @Valid @RequestBody ProjectMemberRoleAddRequest req,
      HttpServletRequest httpReq) {
    requireManage(memberId, httpReq);
    return service.addRoleToMember(memberId, req.getProjectRole());
  }

  @DeleteMapping("/{role}")
  public void remove(
      @PathVariable Long memberId,
      @PathVariable("role") String role,
      HttpServletRequest httpReq) {
    requireManage(memberId, httpReq);
    service.removeRoleFromMember(memberId, role);
  }

  private void requireManage(Long memberId, HttpServletRequest httpReq) {
    Long projectId = service.resolveProjectIdOfMember(memberId);
    Long currentUserId = authz.currentUserId(httpReq);
    if (!authz.canManageProjectMembers(currentUserId, projectId)) {
      throw new ForbiddenException("无权管理项目成员角色");
    }
  }
}
