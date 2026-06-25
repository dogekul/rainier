/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation.controller;

import com.rainier.projectimplementation.dto.ProjectImplementationDetail;
import com.rainier.projectimplementation.dto.ProjectImplementationUpsertRequest;
import com.rainier.projectimplementation.service.ProjectImplementationService;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.89 — 项目立项后施工内容表单。token-optional, all-users (沿用项目周边端点风格)。
 */
@RestController
@RequestMapping("/api/projects/{projectId}/implementation")
public class ProjectImplementationController {

  private final ProjectImplementationService service;

  public ProjectImplementationController(ProjectImplementationService service) {
    this.service = service;
  }

  @GetMapping
  public ProjectImplementationDetail get(@PathVariable Long projectId) {
    return service.findByProjectId(projectId);
  }

  @PutMapping
  public ProjectImplementationDetail upsert(
      @PathVariable Long projectId, @Valid @RequestBody ProjectImplementationUpsertRequest req) {
    return service.createOrUpdate(projectId, req);
  }
}
