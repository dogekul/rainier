/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.demandrequirement.dto.SourceDemandView;
import com.rainier.demandrequirement.service.DemandRequirementLinkService;
import com.rainier.requirement.dto.RequirementCreateRequest;
import com.rainier.requirement.dto.RequirementDetail;
import com.rainier.requirement.dto.RequirementUpdateRequest;
import com.rainier.requirement.service.RequirementService;
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

/** REST endpoints for {@link com.rainier.requirement.domain.Requirement}. */
@RestController
@RequestMapping("/api/requirements")
public class RequirementController {

  private final RequirementService service;
  private final DemandRequirementLinkService linkService;

  public RequirementController(
      RequirementService service, DemandRequirementLinkService linkService) {
    this.service = service;
    this.linkService = linkService;
  }

  @PostMapping
  public ResponseEntity<RequirementDetail> create(
      @Valid @RequestBody RequirementCreateRequest req) {
    RequirementDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/requirements/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public RequirementDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<RequirementDetail> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) Long projectId,
      @Valid PageParams page) {
    return service.list(status, priority, projectId, page);
  }

  @PutMapping("/{id}")
  public RequirementDetail update(
      @PathVariable Long id, @Valid @RequestBody RequirementUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  /** Auxiliary: list demands linked as sources of this requirement (TC-DRL-006). */
  @GetMapping("/{id}/source-demands")
  public List<SourceDemandView> getSourceDemands(@PathVariable Long id) {
    return linkService.findSourceDemands(id);
  }
}
