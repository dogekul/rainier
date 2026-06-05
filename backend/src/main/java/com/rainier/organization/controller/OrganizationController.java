/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.dto.OrganizationCreateRequest;
import com.rainier.organization.dto.OrganizationDetail;
import com.rainier.organization.dto.OrganizationMoveRequest;
import com.rainier.organization.dto.OrganizationUpdateRequest;
import com.rainier.organization.service.OrganizationService;
import java.net.URI;
import java.util.List;
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

/** REST endpoints for the organization tree. */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

  private final OrganizationService service;

  public OrganizationController(OrganizationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OrganizationDetail> create(
      @Valid @RequestBody OrganizationCreateRequest req) {
    OrganizationDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/organizations/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public OrganizationDetail get(@PathVariable String id) {
    return service.findById(id);
  }

  @GetMapping("/tree")
  public List<OrganizationDetail> tree() {
    return service.findTree();
  }

  @GetMapping
  public PageResponse<OrganizationDetail> list(
      @RequestParam(required = false) OrganizationType type,
      @RequestParam(name = "parentId", required = false) String parentId,
      @Valid PageParams page) {
    return service.list(type, parentId, page);
  }

  @PutMapping("/{id}")
  public OrganizationDetail update(
      @PathVariable String id, @Valid @RequestBody OrganizationUpdateRequest req) {
    return service.update(id, req);
  }

  @PutMapping("/{id}/parent")
  public OrganizationDetail move(
      @PathVariable String id, @Valid @RequestBody OrganizationMoveRequest req) {
    return service.move(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
