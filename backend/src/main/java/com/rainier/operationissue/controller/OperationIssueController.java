/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.controller;

import com.rainier.operationissue.dto.OperationIssueCreateRequest;
import com.rainier.operationissue.dto.OperationIssueDetail;
import com.rainier.operationissue.dto.OperationIssueUpdateRequest;
import com.rainier.operationissue.service.OperationIssueService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.58 — 运营问题清单 REST。
 *
 * <p>分两个根：以 operation 维度的 list/create 挂 {@code /api/operations/{id}/issues}；条目读改删挂
 * {@code /api/operation-issues/{id}}（与 demand-requirement-link 同模式）。
 */
@RestController
public class OperationIssueController {

  private final OperationIssueService service;

  public OperationIssueController(OperationIssueService service) {
    this.service = service;
  }

  @GetMapping("/api/operations/{operationId}/issues")
  public List<OperationIssueDetail> listByOperation(@PathVariable Long operationId) {
    return service.listByOperation(operationId);
  }

  @PostMapping("/api/operations/{operationId}/issues")
  public ResponseEntity<OperationIssueDetail> create(
      @PathVariable Long operationId, @Valid @RequestBody OperationIssueCreateRequest req) {
    OperationIssueDetail created = service.create(operationId, req);
    return ResponseEntity.created(URI.create("/api/operation-issues/" + created.getId()))
        .body(created);
  }

  @GetMapping("/api/operation-issues/{id}")
  public OperationIssueDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @PutMapping("/api/operation-issues/{id}")
  public OperationIssueDetail update(
      @PathVariable Long id, @Valid @RequestBody OperationIssueUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/api/operation-issues/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
