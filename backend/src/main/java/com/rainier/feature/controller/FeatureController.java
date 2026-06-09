/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.feature.dto.FeatureCreateRequest;
import com.rainier.feature.dto.FeatureDetail;
import com.rainier.feature.dto.FeatureUpdateRequest;
import com.rainier.feature.service.FeatureService;
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

@RestController
@RequestMapping("/api/features")
public class FeatureController {

  private final FeatureService service;

  public FeatureController(FeatureService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<FeatureDetail> create(@Valid @RequestBody FeatureCreateRequest req) {
    FeatureDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/features/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public FeatureDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<FeatureDetail> list(
      @RequestParam(required = false) Long moduleId,
      @RequestParam(required = false) String status,
      PageParams page) {
    return service.list(moduleId, status, page);
  }

  @PutMapping("/{id}")
  public FeatureDetail update(
      @PathVariable Long id, @Valid @RequestBody FeatureUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
