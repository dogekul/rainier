/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.sprintfeature.dto.SprintFeatureLinkCreateRequest;
import com.rainier.sprintfeature.dto.SprintFeatureLinkDetail;
import com.rainier.sprintfeature.service.SprintFeatureLinkService;
import java.net.URI;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for {@link com.rainier.sprintfeature.domain.SprintFeatureLink}. */
@RestController
@RequestMapping("/api/sprint-features")
public class SprintFeatureLinkController {

  private final SprintFeatureLinkService service;

  public SprintFeatureLinkController(SprintFeatureLinkService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<SprintFeatureLinkDetail> create(
      @Valid @RequestBody SprintFeatureLinkCreateRequest req) {
    SprintFeatureLinkDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/sprint-features/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public SprintFeatureLinkDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<SprintFeatureLinkDetail> list(
      @RequestParam(required = false) Long sprintId,
      @RequestParam(required = false) Long featureId,
      @Valid PageParams page) {
    return service.list(sprintId, featureId, page);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
