/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.dto.ProjectCreateRequest;
import com.rainier.project.dto.ProjectDetail;
import com.rainier.project.dto.ProjectUpdateRequest;
import com.rainier.project.service.ProjectService;
import java.net.URI;
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

  public ProjectController(ProjectService service) {
    this.service = service;
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
  public ProjectDetail update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
