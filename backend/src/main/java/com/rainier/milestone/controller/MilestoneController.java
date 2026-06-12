/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.milestone.dto.MilestoneCreateRequest;
import com.rainier.milestone.dto.MilestoneDetail;
import com.rainier.milestone.dto.MilestoneUpdateRequest;
import com.rainier.milestone.service.MilestoneService;
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

/** REST endpoints for {@link com.rainier.milestone.domain.Milestone}. */
@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

  private final MilestoneService service;

  public MilestoneController(MilestoneService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<MilestoneDetail> create(@Valid @RequestBody MilestoneCreateRequest req) {
    MilestoneDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/milestones/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public MilestoneDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<MilestoneDetail> list(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) String status,
      @Valid PageParams page) {
    return service.list(projectId, status, page);
  }

  @PutMapping("/{id}")
  public MilestoneDetail update(
      @PathVariable Long id, @Valid @RequestBody MilestoneUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
