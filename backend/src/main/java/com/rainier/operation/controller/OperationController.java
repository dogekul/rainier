/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.operation.dto.OperationCreateRequest;
import com.rainier.operation.dto.OperationDetail;
import com.rainier.operation.dto.OperationUpdateRequest;
import com.rainier.operation.service.OperationService;
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

/** 售后运营 endpoints (v0.0.44). all-users (token-gated). */
@RestController
@RequestMapping("/api/operations")
public class OperationController {

  private final OperationService service;

  public OperationController(OperationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OperationDetail> create(@Valid @RequestBody OperationCreateRequest req) {
    OperationDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/operations/" + created.getId())).body(created);
  }

  @GetMapping
  public PageResponse<OperationDetail> list(
      @RequestParam(required = false) String stage,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long opportunityId,
      @Valid PageParams page) {
    return service.list(stage, status, opportunityId, page);
  }

  @GetMapping("/{id}")
  public OperationDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @PutMapping("/{id}")
  public OperationDetail update(
      @PathVariable Long id, @Valid @RequestBody OperationUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/advance")
  public OperationDetail advance(@PathVariable Long id) {
    return service.advance(id);
  }
}
